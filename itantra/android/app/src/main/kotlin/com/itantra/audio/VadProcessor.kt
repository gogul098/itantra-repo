package com.itantra.audio

import ai.onnxruntime.OnnxTensor
import ai.onnxruntime.OrtEnvironment
import ai.onnxruntime.OrtSession
import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import timber.log.Timber
import java.nio.FloatBuffer
import java.nio.LongBuffer

class VadProcessor(context: Context) {
    private val ortEnv = OrtEnvironment.getEnvironment()
    private val ortSession: OrtSession
    private val sampleRate = 16000L

    init {
        val assetManager = context.assets
        val modelBytes = assetManager.open("silero_vad.onnx").readBytes()
        val options = OrtSession.SessionOptions()
        ortSession = ortEnv.createSession(modelBytes, options)
        Timber.i("Silero VAD ONNX model loaded")
    }

    // State tensors for VAD (h and c)
    private var hTensor = Array(2) { Array(1) { FloatArray(64) } }
    private var cTensor = Array(2) { Array(1) { FloatArray(64) } }

    private fun resetState() {
        hTensor = Array(2) { Array(1) { FloatArray(64) } }
        cTensor = Array(2) { Array(1) { FloatArray(64) } }
    }

    fun processStream(audioFlow: Flow<ShortArray>): Flow<ShortArray> = flow {
        resetState()
        var isSpeechActive = false
        var speechProbHistory = mutableListOf<Float>()
        val utteranceBuffer = mutableListOf<Short>()
        var framesSinceSpeech = 0

        audioFlow.collect { chunk ->
            // Convert ShortArray to FloatArray (-1.0 to 1.0)
            val floatChunk = FloatArray(chunk.size) { i -> chunk[i].toFloat() / 32768.0f }
            val prob = inferVad(floatChunk)
            
            speechProbHistory.add(prob)
            if (speechProbHistory.size > 3) speechProbHistory.removeAt(0)

            val recentProb = speechProbHistory.average()

            if (!isSpeechActive) {
                if (recentProb > 0.5) {
                    isSpeechActive = true
                    Timber.d("SPEECH_START detected")
                    utteranceBuffer.clear()
                    framesSinceSpeech = 0
                }
            }

            if (isSpeechActive) {
                utteranceBuffer.addAll(chunk.toList())
                if (prob < 0.3) {
                    framesSinceSpeech++
                } else {
                    framesSinceSpeech = 0
                }

                // ~800ms of silence @ 512 samples per chunk (16kHz) = ~25 frames
                if (framesSinceSpeech > 25) {
                    isSpeechActive = false
                    Timber.d("SPEECH_END detected. Utterance length: ${utteranceBuffer.size}")
                    
                    // Emit the complete utterance
                    emit(utteranceBuffer.toShortArray())
                    utteranceBuffer.clear()
                    framesSinceSpeech = 0
                    resetState() // Reset VAD state for next utterance
                }
            }
        }
    }.flowOn(Dispatchers.Default)

    private fun inferVad(chunk: FloatArray): Float {
        // Silero VAD v4 expects inputs: input (batch, seq_len), sr (1), h (2, batch, 64), c (2, batch, 64)
        val inputTensor = OnnxTensor.createTensor(ortEnv, arrayOf(chunk))
        val srTensor = OnnxTensor.createTensor(ortEnv, LongBuffer.wrap(longArrayOf(sampleRate)), longArrayOf(1))
        val hIn = OnnxTensor.createTensor(ortEnv, hTensor)
        val cIn = OnnxTensor.createTensor(ortEnv, cTensor)

        val inputs = mapOf(
            "input" to inputTensor,
            "sr" to srTensor,
            "h" to hIn,
            "c" to cIn
        )

        try {
            ortSession.run(inputs).use { result ->
                val outProb = result[0].value as Array<FloatArray>
                val hn = result[1].value as Array<Array<FloatArray>>
                val cn = result[2].value as Array<Array<FloatArray>>

                // Update state
                hTensor = hn
                cTensor = cn

                return outProb[0][0]
            }
        } finally {
            inputTensor.close()
            srTensor.close()
            hIn.close()
            cIn.close()
        }
    }

    fun close() {
        ortSession.close()
        ortEnv.close()
    }
}
