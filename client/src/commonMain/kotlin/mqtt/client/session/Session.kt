@file:Suppress("EXPERIMENTAL_API_USAGE")

package mqtt.client.session

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import mqtt.client.platform.PlatformSocketConnection
import mqtt.client.transport.OnMessageReceivedCallback
import mqtt.client.transport.SocketTransport
import mqtt.connection.ConnectionState
import mqtt.connection.IRemoteHost
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

class ClientSession(
    val remoteHost: IRemoteHost,
    override val coroutineContext: CoroutineContext,
    val state: ClientSessionState = ClientSessionState()
) : CoroutineScope, OnMessageReceivedCallback {
    var transport: SocketTransport? = null
    var callback: OnMessageReceivedCallback? = null
    var everyRecvMessageCallback: OnMessageReceivedCallback? = null
    var connack: IConnectionAcknowledgment? = null
    var outboundCallback: ((ControlPacket, Int) -> Unit)? = null

    suspend fun connect(): ConnectionState {
        val transportLocal = transport
        if (transportLocal != null) {
            println("transportLocal $transportLocal")
            return transportLocal.state.value
        }
        val platformSocketConnection = PlatformSocketConnection(remoteHost, coroutineContext)
        platformSocketConnection.outboundCallback = outboundCallback
        this@ClientSession.transport = platformSocketConnection
        platformSocketConnection.messageReceiveCallback = this@ClientSession
        val state = platformSocketConnection.openConnectionAsync(true).await()
        val connack = platformSocketConnection.connack
        this.connack = connack
        if (!remoteHost.request.cleanStart && connack != null && connack.isSuccessful && connack.sessionPresent) {
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
                        val topicName = controlPacket.topic.validateTopic()
                        if (topicName == null) {
                            println("Failed to validate the topic")
                            return@launch
                        }
                        state.subscriptionManager.handleIncomingPublish(controlPacket)
                        val response = controlPacket.expectedResponse() ?: return@launch
                        send(response)
                    }
                    is IPublishAcknowledgment ->
                        state.qos1And2MessagesSentButNotAcked.remove(controlPacket.packetIdentifier.toUShort())
                    is IPublishReceived -> {
                        callback?.onMessage(controlPacket)
                        state.qos1And2MessagesSentButNotAcked.remove(controlPacket.packetIdentifier.toUShort())
                        val pubRel = controlPacket.expectedResponse()
                        send(pubRel)
                        state.qos2MessagesRecevedButNotCompletelyAcked.put(pubRel.packetIdentifier.toUShort(), pubRel)
                    }
                    is IPublishRelease -> {
                        state.qos2MessagesRecevedButNotCompletelyAcked.remove(controlPacket.packetIdentifier.toUShort())
                        send(controlPacket.expectedResponse())
                    }
                    is IPublishComplete ->
                        state.qos2MessagesRecevedButNotCompletelyAcked.remove(controlPacket.packetIdentifier.toUShort())
                    is ISubscribeAcknowledgement -> state.subscriptionAcknowledgementReceived(controlPacket)
                    else -> {
                        callback?.onMessage(controlPacket)
                    }
                }
            } catch (e: Exception) {
                println("Application failed to process $controlPacket")
                println(e)
            } finally {
                everyRecvMessageCallback?.onMessage(controlPacket)
            }
        }
    }

    suspend fun awaitSocketClose() {
        transport?.awaitSocketClose()
        this.transport = null
    }

    suspend fun publish(topic: String, qos: QualityOfService,
                        packetIdentifier: UShort = getAndIncrementPacketIdentifier().toUShort()
    ) {
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

    suspend fun <T : Any> publish(topic: String, qos: QualityOfService, typeClass: KClass<T>, payload: T) {
        val actualPayload = run {
            val serializer =
                findSerializer(typeClass) ?: throw RuntimeException("Failed to find serializer for $payload")
            serializer.serialize(payload)
        }
        send(PublishMessage(topic, qos, actualPayload))
    }


    suspend fun <T : Any> subscribe(
        topic: Filter, qos: QualityOfService, typeClass: KClass<T>,
        callback: SubscriptionCallback<T>
    ) {
        val node = topic.validate() ?: return
        state.subscriptionManager.register(node, typeClass, callback)
        val subscription = SubscribeRequest(10.toUShort(), topic, qos)
        send(subscription)
        state.sentSubscriptionRequest(subscription, typeClass, listOf(callback))
    }

    suspend inline fun <reified T : Any> subscribe(
        topic: Filter,
        qos: QualityOfService,
        callback: SubscriptionCallback<T>
    ) = subscribe(topic, qos, T::class, callback)

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
        val transport = transport ?: return
        transport.clientToServer.send(msg)
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
    }

    suspend fun unsubscribe(topics: List<String>) =
            send(UnsubscribeRequest(topics = topics.map { MqttUtf8String(it) }))

    suspend fun disconnectAsync(): Boolean {
        val result = transport?.closeAsync()?.await() ?: false
        transport = null
        return result
    }
}
