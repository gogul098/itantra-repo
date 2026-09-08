package com.itantra.semantic

import ai.onnxruntime.OnnxTensor
import ai.onnxruntime.OrtEnvironment
import ai.onnxruntime.OrtSession
import android.content.Context
import timber.log.Timber
import java.nio.LongBuffer

class SonarEncoder(context: Context) {
    private val ortEnv = OrtEnvironment.getEnvironment()
    private var ortSession: OrtSession? = null

    init {
        try {
            val assetManager = context.assets
            val modelBytes = assetManager.open("sonar_encoder.onnx").readBytes()
            val options = OrtSession.SessionOptions()
            ortSession = ortEnv.createSession(modelBytes, options)
            Timber.i("SONAR Encoder ONNX model loaded")
        } catch (e: Exception) {
            Timber.w(e, "SONAR Encoder model not found in assets. Running in Mock Mode.")
        }
    }

    suspend fun encode(text: String, sourceLang: String): FloatArray {
        Timber.d("Encoding text: '$text' in language: $sourceLang")
        
        // 1. Tokenize (Mocking SentencePiece tokenization here)
        // In real app, we would use a SentencePiece Android port or custom JNI wrapper
        val dummyTokens = longArrayOf(101L, 202L, 303L, 404L) // mock token IDs
        
        // 2. Run Inference
        val inputIdsTensor = OnnxTensor.createTensor(
            ortEnv, 
            LongBuffer.wrap(dummyTokens), 
            longArrayOf(1, dummyTokens.size.toLong())
        )
        
        return try {
            val session = ortSession
            if (session == null) {
                Timber.w("Mocking SONAR Encoder output (model missing)")
                return FloatArray(1024)
            }
            val inputs = mapOf("input_ids" to inputIdsTensor)
            session.run(inputs).use { result ->
                // Output shape is expected to be [1, 1024]
                val output = result[0].value as Array<FloatArray>
                output[0]
            }
        } catch (e: Exception) {
            Timber.e(e, "SONAR Encoding failed")
            // Return dummy vector on failure to not crash the pipeline for now
            FloatArray(1024)
        } finally {
            inputIdsTensor.close()
        }
    }

    fun close() {
        ortSession?.close()
        ortEnv.close()
    }
}
