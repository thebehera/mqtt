package mqtt.client.session

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.async
import kotlinx.coroutines.launch
import mqtt.client.connection.ConnectionParameters
import mqtt.client.platform.PlatformSocketConnection
import mqtt.client.subscription.SubscriptionManager
import mqtt.client.transport.SocketTransport
import mqtt.wire.control.packet.ControlPacket
import kotlin.coroutines.CoroutineContext

class ClientSession(val params: ConnectionParameters, override val coroutineContext: CoroutineContext) : CoroutineScope {
    var transport: SocketTransport? = null
    val state = ClientSessionState()
    val subscriptionManager = SubscriptionManager()

    fun connectAsync() = async {
        val session = PlatformSocketConnection(params, coroutineContext)
        this@ClientSession.transport = session
        session.openConnectionAsync(true).await().value
    }

    suspend fun awaitSocketClose() {
        transport?.awaitSocketClose()
        this.transport = null
    }

    fun send(msg: ControlPacket) {
        val warning = msg.validateOrGetWarning()
        if (warning != null) {
            throw warning
        }
//        val session = transport ?: return
//        state.messagesNotSent.offer(msg)
        launch {
            transport!!.clientToServer.send(msg)
        }
    }

    fun disconnectAsync(): Deferred<Boolean>? {
        val result = transport?.closeAsync()
        transport = null
        return result
    }

}
