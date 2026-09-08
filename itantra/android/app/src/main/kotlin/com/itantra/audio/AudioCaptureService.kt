package com.itantra.audio

import android.annotation.SuppressLint
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import timber.log.Timber

class AudioCaptureService {
    private val sampleRate = 16000
    private val channelConfig = AudioFormat.CHANNEL_IN_MONO
    private val audioFormat = AudioFormat.ENCODING_PCM_16BIT
    private val bufferSize = AudioRecord.getMinBufferSize(sampleRate, channelConfig, audioFormat)
    private val chunkSize = 512 // matches VAD input chunk size

    @Volatile
    private var isRecording = false

    @SuppressLint("MissingPermission")
    fun startCapture(): Flow<ShortArray> = flow {
        isRecording = true
        var audioRecord: AudioRecord? = null
        try {
            audioRecord = AudioRecord(
                MediaRecorder.AudioSource.MIC,
                sampleRate,
                channelConfig,
                audioFormat,
                bufferSize.coerceAtLeast(chunkSize * 2)
            )

            if (audioRecord.state != AudioRecord.STATE_INITIALIZED) {
                Timber.e("AudioRecord initialization failed")
                return@flow
            }

            audioRecord.startRecording()
            Timber.i("Started audio capture")

            val audioBuffer = ShortArray(chunkSize)
            while (isRecording) {
                val readResult = audioRecord.read(audioBuffer, 0, chunkSize)
                if (readResult > 0) {
                    // Create a copy to emit to avoid concurrent modification issues downstream
                    emit(audioBuffer.copyOf(readResult))
                } else {
                    Timber.w("AudioRecord read error: $readResult")
                }
            }
        } catch (e: Exception) {
            Timber.e(e, "Error during audio capture")
        } finally {
            audioRecord?.stop()
            audioRecord?.release()
            Timber.i("Stopped audio capture")
        }
    }.flowOn(Dispatchers.IO)

    fun stopCapture() {
        isRecording = false
    }
}
