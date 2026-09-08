package com.itantra.transport

import android.annotation.SuppressLint
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothSocket
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import timber.log.Timber
import java.io.InputStream
import java.io.OutputStream
import java.nio.ByteBuffer
import java.util.UUID

class RfcommTransport(private val device: BluetoothDevice) : Transport {

    // Standard SerialPortService ID
    private val SPP_UUID: UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB")

    private val _incomingFlow = MutableSharedFlow<ByteArray>(extraBufferCapacity = 64)
    override val incomingFlow: Flow<ByteArray> = _incomingFlow

    @SuppressLint("MissingPermission")
    override suspend fun send(payload: ByteArray): Result<Unit> = withContext(Dispatchers.IO) {
        var socket: BluetoothSocket? = null
        try {
            socket = device.createRfcommSocketToServiceRecord(SPP_UUID)
            socket.connect()
            
            val outStream: OutputStream = socket.outputStream
            val inStream: InputStream = socket.inputStream

            // Frame format: [ 0xAA (1 byte) | len (2 bytes) | payload | CRC16 (2 bytes) ]
            val header = ByteBuffer.allocate(3)
                .put(0xAA.toByte())
                .putShort(payload.size.toShort())
                .array()
            
            val crc = ByteBuffer.allocate(2).putShort(0).array() // Mock CRC
            
            val packet = header + payload + crc
            
            outStream.write(packet)
            outStream.flush()
            
            // Await ACK (1 byte)
            val ack = inStream.read()
            if (ack == 0x01) {
                Timber.d("Received ACK from RFCOMM")
                Result.success(Unit)
            } else {
                Timber.w("Failed to receive ACK via RFCOMM, got: $ack")
                Result.failure(Exception("ACK missing"))
            }
        } catch (e: Exception) {
            Timber.e(e, "RFCOMM connection/send failed")
            Result.failure(e)
        } finally {
            socket?.close()
        }
    }

    @SuppressLint("MissingPermission")
    fun startServer(adapter: android.bluetooth.BluetoothAdapter) {
        kotlinx.coroutines.CoroutineScope(Dispatchers.IO).launch {
            var serverSocket: android.bluetooth.BluetoothServerSocket? = null
            try {
                serverSocket = adapter.listenUsingRfcommWithServiceRecord("iTantra", SPP_UUID)
                Timber.i("BLE RFCOMM Server listening")
                
                while (true) {
                    val socket = serverSocket.accept()
                    Timber.d("RFCOMM Client connected")
                    handleIncomingConnection(socket)
                }
            } catch (e: Exception) {
                Timber.e(e, "RFCOMM server failed")
            } finally {
                serverSocket?.close()
            }
        }
    }

    private suspend fun handleIncomingConnection(socket: BluetoothSocket) = withContext(Dispatchers.IO) {
        try {
            val inStream = socket.inputStream
            val outStream = socket.outputStream
            
            // Read 3-byte header
            val header = ByteArray(3)
            var bytesRead = inStream.read(header)
            if (bytesRead == 3 && header[0] == 0xAA.toByte()) {
                val len = ByteBuffer.wrap(header, 1, 2).short.toInt()
                
                // Read payload + CRC
                val payloadBuffer = ByteArray(len + 2)
                var totalRead = 0
                while (totalRead < len + 2) {
                    val read = inStream.read(payloadBuffer, totalRead, len + 2 - totalRead)
                    if (read == -1) break
                    totalRead += read
                }
                
                if (totalRead == len + 2) {
                    // Send ACK
                    outStream.write(byteArrayOf(0x01))
                    outStream.flush()
                    
                    // Emit to flow (excluding CRC for now)
                    _incomingFlow.emit(payloadBuffer.sliceArray(0 until len))
                }
            }
        } catch (e: Exception) {
            Timber.e(e, "Error handling RFCOMM connection")
        } finally {
            socket.close()
        }
    }
}
