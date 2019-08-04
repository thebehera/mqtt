@file:Suppress("EXPERIMENTAL_API_USAGE")

package mqtt.client.session

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import mqtt.client.connection.ConnectionParameters
import mqtt.client.connection.ConnectionState
import mqtt.client.platform.PlatformSocketConnection
import mqtt.client.transport.OnMessageReceivedCallback
import mqtt.client.transport.SocketTransport
import mqtt.wire.control.packet.*
import mqtt.wire.data.MqttUtf8String
import mqtt.wire.data.QualityOfService
import mqtt.wire.data.topic.Filter
import mqtt.wire.data.topic.SubscriptionCallback
import mqtt.wire4.control.packet.PublishMessage
import mqtt.wire4.control.packet.SubscribeRequest
import mqtt.wire4.control.packet.UnsubscribeRequest
import kotlin.coroutines.CoroutineContext
import kotlin.reflect.KClass

class ClientSession(val params: ConnectionParameters,
                    override val coroutineContext: CoroutineContext,
                    val state: ClientSessionState) : CoroutineScope, OnMessageReceivedCallback {
    var transport: SocketTransport? = null
    var callback: OnMessageReceivedCallback? = null
    var everyRecvMessageCallback: OnMessageReceivedCallback? = null

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

    private fun fireCallbackOrPrint(controlPacket: ControlPacket, callback: OnMessageReceivedCallback?) {
        if (callback == null) {
            if (controlPacket is IPublishMessage && params.logIncomingPublish || params.logIncomingControlPackets) {
                println("IN: $controlPacket")
            }
        } else {
            callback.onMessage(controlPacket)
        }
    }

    override fun onMessage(controlPacket: ControlPacket) {
        launch {
            var printed = false
            try {
                when (controlPacket) {
                    is IPublishMessage -> {
                        fireCallbackOrPrint(controlPacket, callback)
                        val topicName = controlPacket.topic.validateTopic()
                        if (topicName == null) {
                            println("Failed to validate the topic")
                            return@launch
                        }
                        state.subscriptionManager.handleIncomingPublish(controlPacket)
                        printed = true
                        val response = controlPacket.expectedResponse() ?: return@launch
                        send(response)
                    }
                    is IPublishAcknowledgment ->
                        state.qos1And2MessagesSentButNotAcked.remove(controlPacket.packetIdentifier)
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
                    is IPublishComplete ->
                        state.qos2MessagesRecevedButNotCompletelyAcked.remove(controlPacket.packetIdentifier)
                    is ISubscribeAcknowledgement -> state.subscriptionAcknowledgementReceived(controlPacket)
                    else -> {
                        fireCallbackOrPrint(controlPacket, callback)
                        printed = true
                    }
                }
            } catch (e: Exception) {
                println("Application failed to process $controlPacket")
                println(e)
            } finally {
                if (printed) {
                    everyRecvMessageCallback?.onMessage(controlPacket)
                } else {
                    fireCallbackOrPrint(controlPacket, everyRecvMessageCallback)
                }
            }
        }
    }

    suspend fun awaitSocketClose() {
        transport?.awaitSocketClose()
        this.transport = null
    }

    suspend fun publish(topic: String, qos: QualityOfService,
                        packetIdentifier: UShort = getAndIncrementPacketIdentifier()) {
        send(PublishMessage(topic, qos, packetIdentifier))
    }

    suspend inline fun <reified T : Any> publishGeneric(topic: String, qos: QualityOfService, payload: T) = publish(topic, qos, payload)

    suspend inline fun <reified T : Any> publish(topic: String, qos: QualityOfService, payload: T) {
        val actualPayload = run {
            val serializer = findSerializer<T>() ?: throw RuntimeException("Failed to find serializer for $payload")
            serializer.serialize(payload)
        }
        send(PublishMessage(topic, qos, actualPayload))
    }

    suspend fun publish(topic: String, qos: QualityOfService, payload: Any, payloadType: KClass<Any>) {
        val actualPayload = run {
            val serializer =
                findSerializer(payloadType) ?: throw RuntimeException("Failed to find serializer for $payload")
            serializer.serialize(payload)
        }
        send(PublishMessage(topic, qos, actualPayload))
    }



    suspend inline fun <reified T : Any> subscribe(topic: Filter, qos: QualityOfService, callback: SubscriptionCallback<T>) {
        val node = topic.validate() ?: return
        state.subscriptionManager.register(node, callback)
        val subscription = SubscribeRequest(10.toUShort(), topic, qos)
        send(subscription)
        state.sentSubscriptionRequest(subscription, listOf(callback))
    }

    suspend inline fun <reified T : Any> subscribe(topics: List<Filter>, qos: List<QualityOfService>, callbacks: List<SubscriptionCallback<T>>) {
        if (topics.size != qos.size && qos.size != callbacks.size) {
            throw IllegalArgumentException("Failed to subscribe: Topics.size != qos.size != callbacks.size")
        }
        val size = topics.size
        val subscription = SubscribeRequest(topics, qos)
        for (index in 0..size) {
            val node = topics[index].validate() ?: return
            state.subscriptionManager.register(node, callbacks[index])
        }
        send(subscription)
        state.sentSubscriptionRequest(subscription, callbacks)
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

    suspend fun unsubscribe(topics: List<String>) =
            send(UnsubscribeRequest(topics = topics.map { MqttUtf8String(it) }))

    suspend fun disconnectAsync(): Boolean? {
        val result = transport?.closeAsync()?.await() ?: false
        transport = null
        return result
    }
}