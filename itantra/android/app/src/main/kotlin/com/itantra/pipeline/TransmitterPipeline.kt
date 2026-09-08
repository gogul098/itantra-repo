package com.itantra.pipeline

import com.itantra.asr.AsrEngine
import com.itantra.audio.AudioCaptureService
import com.itantra.audio.VadProcessor
import com.itantra.semantic.QuantizationUtils
import com.itantra.semantic.SonarEncoder
import com.itantra.transport.TransportManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import timber.log.Timber

class TransmitterPipeline(
    private val audioCapture: AudioCaptureService,
    private val vadProcessor: VadProcessor,
    private val asrEngine: AsrEngine,
    private val sonarEncoder: SonarEncoder,
    private val transportManager: TransportManager
) {
    private var pipelineJob: Job? = null

    fun startTransmission(sourceLang: String, targetLang: String) {
        if (pipelineJob?.isActive == true) {
            Timber.w("Transmission already in progress")
            return
        }

        pipelineJob = CoroutineScope(Dispatchers.Default).launch {
            Timber.i("Starting Transmitter Pipeline. Source: $sourceLang, Target: $targetLang")
            
            val rawAudioFlow = audioCapture.startCapture()
            val utteranceFlow = vadProcessor.processStream(rawAudioFlow)

            utteranceFlow.collect { utterance ->
                Timber.d("Processing completed utterance")
                val asrResult = asrEngine.transcribe(utterance)
                
                // Confidence gate handles null returns
                if (asrResult != null) {
                    val embedding = sonarEncoder.encode(asrResult.text, sourceLang)
                    
                    // The payload is 1028 bytes (1024 int8 + 4 float scale)
                    val packet = QuantizationUtils.quantize(embedding)
                    
                    // Attach target lang string so receiver knows what to decode into (4 bytes)
                    // Padded/truncated to exactly 4 chars for consistent parsing (e.g., 'tam_')
                    val targetLangBytes = targetLang.take(4).padEnd(4, '_').encodeToByteArray()
                    
                    val finalPayload = packet + targetLangBytes
                    
                    val result = transportManager.send(finalPayload)
                    if (result.isSuccess) {
                        Timber.i("Successfully transmitted utterance embedding")
                    } else {
                        Timber.e("Failed to transmit utterance")
                    }
                }
            }
        }
    }

    fun stopTransmission() {
        Timber.i("Stopping Transmitter Pipeline")
        audioCapture.stopCapture()
        pipelineJob?.cancel()
        pipelineJob = null
    }
}
