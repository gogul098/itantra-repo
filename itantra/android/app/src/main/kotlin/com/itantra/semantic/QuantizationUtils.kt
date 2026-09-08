package com.itantra.semantic

import java.nio.ByteBuffer
import kotlin.math.abs
import kotlin.math.roundToInt

object QuantizationUtils {

    /**
     * Quantizes a 1024-dimensional FloatArray into a 1028-byte payload.
     * The payload consists of 1024 INT8 values followed by a 4-byte Float32 scale factor.
     */
    fun quantize(embedding: FloatArray): ByteArray {
        require(embedding.size == 1024) { "Embedding must be exactly 1024 dimensions" }
        
        var maxAbs = 0f
        for (v in embedding) {
            val a = abs(v)
            if (a > maxAbs) maxAbs = a
        }
        
        // Prevent division by zero
        val scale = if (maxAbs == 0f) 1f else maxAbs / 127f
        
        val int8Array = ByteArray(1024) { i ->
            (embedding[i] / scale).roundToInt().coerceIn(-127, 127).toByte()
        }
        
        val scaleBytes = ByteBuffer.allocate(4).putFloat(scale).array()
        
        return int8Array + scaleBytes // 1024 bytes + 4 bytes = 1028 bytes
    }

    /**
     * Dequantizes a 1028-byte payload back into a 1024-dimensional FloatArray.
     */
    fun dequantize(packet: ByteArray): FloatArray {
        require(packet.size == 1028) { "Packet must be exactly 1028 bytes" }
        
        val scale = ByteBuffer.wrap(packet, 1024, 4).float
        
        return FloatArray(1024) { i ->
            packet[i].toFloat() * scale
        }
    }
}
