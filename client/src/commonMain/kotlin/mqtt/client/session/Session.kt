package mqtt.client.session

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import mqtt.client.connection.ConnectionParameters
import mqtt.client.connection.ConnectionState
import mqtt.client.platform.PlatformSocketConnection
import mqtt.client.transport.OnMessageReceivedCallback
import mqtt.client.transport.SocketTransport
import mqtt.wire.control.packet.*
import mqtt.wire4.control.packet.SubscribeRequest
import kotlin.coroutines.CoroutineContext

class ClientSession(val params: ConnectionParameters,
                    override val coroutineContext: CoroutineContext,
                    val state: ClientSessionState) : CoroutineScope, OnMessageReceivedCallback {
    var transport: SocketTransport? = null
    var callback: OnMessageReceivedCallback? = null

    suspend fun connect(): ConnectionState {
        val transportLocal = transport
        if (transportLocal != null) {
            return transportLocal.state.value
        }
        val platformSocketConnection = PlatformSocketConnection(params, coroutineContext)
        this@ClientSession.transport = platformSocketConnection
        platformSocketConnection.messageReceiveCallback = this@ClientSession
        val state = platformSocketConnection.openConnectionAsync(true).await()
        val connack = platformSocketConnection.connack

        if (!params.connectionRequest.cleanStart && connack != null && connack.isSuccessful && connack.sessionPresent) {
            flushQueues()
        }
        return state.value
    }

    override fun onMessage(controlPacket: ControlPacket) {
        launch {
            try {
                when (controlPacket) {
                    is IPublishMessage -> {
                        callback?.onMessage(controlPacket)
                        val response = controlPacket.expectedResponse() ?: return@launch
                        send(response)
                    }
                    is IPublishAcknowledgment -> {
                        state.qos1And2MessagesSentButNotAcked.remove(controlPacket.packetIdentifier)
                    }
                    is IPublishReceived -> {
                        callback?.onMessage(controlPacket)
                        state.qos1And2MessagesSentButNotAcked.remove(controlPacket.packetIdentifier)
                        val pubRel = controlPacket.expectedResponse()
                        send(pubRel)
                        state.qos2MessagesRecevedButNotCompletelyAcked.put(pubRel.packetIdentifier, pubRel)
                    }
                    is IPublishRelease -> {
                        state.qos2MessagesRecevedButNotCompletelyAcked.remove(controlPacket.packetIdentifier)
                        send(controlPacket.expectedResponse())
                    }
                    is IPublishComplete -> {
                        state.qos2MessagesRecevedButNotCompletelyAcked.remove(controlPacket.packetIdentifier)
                    }
                    is ISubscribeAcknowledgement -> {

                    }
                    else -> callback?.onMessage(controlPacket)
                }
            } catch (e: Exception) {
                println("Application failed to process $controlPacket")
                println(e)
            }
        }
    }

    suspend fun awaitSocketClose() {
        transport?.awaitSocketClose()
        this.transport = null
    }

    suspend fun send(msg: ControlPacket) {
        val warning = msg.validateOrGetWarning()
        if (warning != null) {
            throw warning
        }
        if (!state.messagesNotSent.offer(msg)) {
            println("Failed to add $msg to the messages not sent queue, messages can be lost due to a power or network failure")
        }
        val transport = transport ?: return
        if (msg is SubscribeRequest) {
            state.unacknowledgedSubscriptions[msg.packetIdentifier] = msg
        }
        transport.clientToServer.send(msg)
        state.messagesNotSent.remove(msg)
    }

    private suspend fun flushQueues() {
        val transport = transport ?: return
        for (key in state.qos2MessagesRecevedButNotCompletelyAcked.keys()) {
            val msg = state.qos2MessagesRecevedButNotCompletelyAcked.get(key) ?: continue
            transport.clientToServer.send(msg)
        }
        for (key in state.qos1And2MessagesSentButNotAcked.keys()) {
            val msg = state.qos1And2MessagesSentButNotAcked.get(key) ?: continue
            transport.clientToServer.send(msg)
        }
        var queuedControlPacket = state.messagesNotSent.peek()
        while (queuedControlPacket != null) {
            transport.clientToServer.send(queuedControlPacket)
            state.messagesNotSent.remove(queuedControlPacket)
            queuedControlPacket = state.messagesNotSent.peek()
        }
    }

    suspend fun disconnectAsync(): Boolean? {
        val result = transport?.closeAsync()?.await() ?: false
        transport = null
        return result
    }
}