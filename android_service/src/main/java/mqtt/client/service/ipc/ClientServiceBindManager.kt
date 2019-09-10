package mqtt.client.service.ipc

import android.os.Messenger
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

class ClientServiceBindManager {
    private val serviceBindSuspendContinuations = SuspendOnIncomingMessageHandler<Messenger>()
    private var serviceMessenger: Messenger? = null

    fun onServiceConnected(serviceMessenger: Messenger) {
        this.serviceMessenger = serviceMessenger
        serviceBindSuspendContinuations.notify(serviceMessenger)
    }

    suspend fun awaitServiceBound(): Messenger = suspendCoroutine { continuation ->
        val messenger = serviceMessenger
        if (messenger != null) {
            continuation.resume(messenger)
        }
        serviceBindSuspendContinuations.queue(continuation)
    }
}