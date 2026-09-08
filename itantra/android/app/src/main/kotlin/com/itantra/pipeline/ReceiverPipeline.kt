package com.itantra.pipeline

import com.itantra.audio.AudioPlaybackService
import com.itantra.cache.CosineCache
import com.itantra.semantic.QuantizationUtils
import com.itantra.semantic.SonarDecoder
import com.itantra.transport.TransportManager
import com.itantra.tts.PiperTtsEngine
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import timber.log.Timber

class ReceiverPipeline(
    private val transportManager: TransportManager,
    private val cosineCache: CosineCache,
    private val sonarDecoder: SonarDecoder,
    private val piperTts: PiperTtsEngine,
    private val audioPlayback: AudioPlaybackService
) {
    private var pipelineJob: Job? = null

    fun startReceiving(targetLang: String) {
        if (pipelineJob?.isActive == true) {
            Timber.w("Receiver pipeline already active")
            return
        }

        pipelineJob = CoroutineScope(Dispatchers.Default).launch {
            Timber.i("Starting Receiver Pipeline. Decoding into: $targetLang")
            
            // Listen for incoming 1032-byte packets (1028 payload + 4 lang code)
            transportManager.incomingFlow.collect { rawPacket ->
                if (rawPacket.size < 1028) {
                    Timber.e("Received malformed packet of size ${rawPacket.size}")
                    return@collect
                }

                Timber.d("Received packet. Processing...")
                
                // Extract 1028 byte embedding payload
                val payload = rawPacket.sliceArray(0 until 1028)
                val senderLangBytes = if (rawPacket.size >= 1032) rawPacket.sliceArray(1028 until 1032) else ByteArray(0)
                
                // We could use senderLang for logging or UI, but it's not strictly needed for decoding
                val senderLang = String(senderLangBytes).trimEnd('_')
                Timber.d("Sender language hint: $senderLang")

                val embedding = try {
                    QuantizationUtils.dequantize(payload)
                } catch (e: Exception) {
                    Timber.e(e, "Failed to dequantize payload")
                    return@collect
                }

                // 1. Fast Path: Cache Lookup
                val cacheHit = cosineCache.lookup(embedding, targetLang)
                if (cacheHit != null) {
                    Timber.i("Cache HIT! Playing pre-rendered audio.")
                    audioPlayback.playFile(cacheHit.audioPath)
                    return@collect
                }

                // 2. Slow Path: Full Decode & Synthesis
                Timber.i("Cache MISS. Running full decode pipeline.")
                val targetText = sonarDecoder.decode(embedding, targetLang)
                
                val pcm = piperTts.synthesize(targetText, targetLang)
                audioPlayback.playPcm(pcm)
            }
        }
    }

    fun stopReceiving() {
        Timber.i("Stopping Receiver Pipeline")
        pipelineJob?.cancel()
        pipelineJob = null
    }
}
