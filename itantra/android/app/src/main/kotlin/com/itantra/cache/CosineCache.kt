package com.itantra.cache

import java.nio.ByteBuffer
import kotlin.math.sqrt

data class CacheHit(val audioPath: String)

class CosineCache(private val db: VectorCacheDb) {

    suspend fun lookup(queryEmbedding: FloatArray, targetLang: String): CacheHit? {
        val candidates = db.cachedPhraseDao().getPhrasesForLang(targetLang)
        
        for (phrase in candidates) {
            val phraseEmbedding = bytesToFloatArray(phrase.embedding)
            val sim = cosineSimilarity(queryEmbedding, phraseEmbedding)
            
            if (sim > 0.95f) {
                db.cachedPhraseDao().incrementHitCount(phrase.id)
                return CacheHit(audioPath = phrase.audioPath)
            }
        }
        return null
    }

    private fun cosineSimilarity(a: FloatArray, b: FloatArray): Float {
        if (a.size != b.size) return 0f
        var dot = 0.0
        var normA = 0.0
        var normB = 0.0
        
        for (i in a.indices) {
            dot += a[i] * b[i]
            normA += a[i] * a[i]
            normB += b[i] * b[i]
        }
        
        if (normA == 0.0 || normB == 0.0) return 0f
        return (dot / (sqrt(normA) * sqrt(normB))).toFloat()
    }

    private fun bytesToFloatArray(bytes: ByteArray): FloatArray {
        val buffer = ByteBuffer.wrap(bytes)
        val floatArray = FloatArray(bytes.size / 4)
        for (i in floatArray.indices) {
            floatArray[i] = buffer.float
        }
        return floatArray
    }
}
