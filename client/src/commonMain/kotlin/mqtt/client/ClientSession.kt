package mqtt.client

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.async
import kotlinx.coroutines.launch
import mqtt.wire.control.packet.ControlPacket
import kotlin.coroutines.CoroutineContext

class ClientSession(val params: ConnectionParameters, override val coroutineContext: CoroutineContext) : CoroutineScope {
    var connection: SocketConnection? = null
    val state = ClientSessionState()
    val subscriptionManager = SubscriptionManager()

    fun connectAsync() = async {
        val session = PlatformSocketConnection(params, coroutineContext)
        this@ClientSession.connection = session
        session.openConnectionAsync(true).await().value
    }

    suspend fun awaitSocketClose() {
        connection?.awaitSocketClose()
        this.connection = null
    }

    fun send(msg: ControlPacket) {
        val warning = msg.validateOrGetWarning()
        if (warning != null) {
            throw warning
        }
//        val session = connection ?: return
//        state.messagesNotSent.offer(msg)
        launch {
            connection!!.clientToServer.send(msg)
        }
    }

    fun disconnectAsync(): Deferred<Boolean>? {
        val result = connection?.closeAsync()
        connection = null
        return result
    }

}
