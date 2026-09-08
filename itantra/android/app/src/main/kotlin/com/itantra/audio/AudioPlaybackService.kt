package com.itantra.audio

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioManager
import android.media.AudioTrack
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import timber.log.Timber
import java.io.File
import java.io.FileInputStream

class AudioPlaybackService(private val context: Context) {
    private val sampleRate = 16000
    private val channelConfig = AudioFormat.CHANNEL_OUT_MONO
    private val audioFormat = AudioFormat.ENCODING_PCM_16BIT
    private val minBufferSize = AudioTrack.getMinBufferSize(sampleRate, channelConfig, audioFormat)

    private val audioTrack: AudioTrack

    init {
        val audioAttributes = AudioAttributes.Builder()
            .setUsage(AudioAttributes.USAGE_VOICE_COMMUNICATION)
            .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
            .build()

        val format = AudioFormat.Builder()
            .setSampleRate(sampleRate)
            .setEncoding(audioFormat)
            .setChannelMask(channelConfig)
            .build()

        audioTrack = AudioTrack.Builder()
            .setAudioAttributes(audioAttributes)
            .setAudioFormat(format)
            .setBufferSizeInBytes(minBufferSize.coerceAtLeast(sampleRate * 2)) // 1 second buffer
            .setTransferMode(AudioTrack.MODE_STREAM)
            .build()
            
        Timber.i("AudioPlaybackService initialized")
    }

    suspend fun playPcm(pcmData: ShortArray) = withContext(Dispatchers.IO) {
        Timber.d("Playing PCM audio array of length ${pcmData.size}")
        try {
            audioTrack.play()
            audioTrack.write(pcmData, 0, pcmData.size)
        } catch (e: Exception) {
            Timber.e(e, "Error playing PCM data")
        } finally {
            if (audioTrack.playState == AudioTrack.PLAYSTATE_PLAYING) {
                audioTrack.stop()
            }
        }
    }

    suspend fun playFile(filePath: String) = withContext(Dispatchers.IO) {
        Timber.d("Playing audio file from path: $filePath")
        try {
            val file = File(filePath)
            if (!file.exists()) {
                Timber.w("Audio file not found: $filePath")
                return@withContext
            }

            audioTrack.play()
            val inputStream = FileInputStream(file)
            // Skip 44-byte WAV header assuming 16kHz mono 16-bit PCM
            inputStream.skip(44)
            
            val buffer = ByteArray(minBufferSize)
            var bytesRead: Int
            while (inputStream.read(buffer).also { bytesRead = it } != -1) {
                audioTrack.write(buffer, 0, bytesRead)
            }
            inputStream.close()
        } catch (e: Exception) {
            Timber.e(e, "Error playing audio file")
        } finally {
            if (audioTrack.playState == AudioTrack.PLAYSTATE_PLAYING) {
                audioTrack.stop()
            }
        }
    }

    fun release() {
        audioTrack.release()
    }
}
