package com.itantra.transport

import android.bluetooth.BluetoothDevice
import android.os.Build
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import timber.log.Timber

interface Transport {
    suspend fun send(payload: ByteArray): Result<Unit>
    val incomingFlow: Flow<ByteArray>
}

class TransportManager(private val device: BluetoothDevice) : Transport {

    private val bleTransport by lazy { BleL2capTransport(device) }
    private val rfcommTransport by lazy { RfcommTransport(device) }
    
    private val _incomingFlow = MutableSharedFlow<ByteArray>(extraBufferCapacity = 64)
    override val incomingFlow: Flow<ByteArray> = _incomingFlow

    override suspend fun send(payload: ByteArray): Result<Unit> {
        // API 29+ -> try BLE L2CAP CoC
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            Timber.d("Attempting to send via BLE L2CAP")
            val result = bleTransport.send(payload)
            if (result.isSuccess) {
                return result
            }
            Timber.w("BLE L2CAP failed, falling back to RFCOMM")
        } else {
            Timber.d("Device is < API 29, using RFCOMM by default")
        }

        // Fallback or API < 29
        return rfcommTransport.send(payload)
    }
}
