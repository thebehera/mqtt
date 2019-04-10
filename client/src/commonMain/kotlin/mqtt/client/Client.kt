package mqtt.client

import kotlinx.atomicfu.AtomicRef
import kotlinx.coroutines.Deferred
import mqtt.client.persistence.Persistence
import mqtt.wire.control.packet.findSerializer
import mqtt.wire.data.MqttUtf8String
import mqtt.wire.data.QualityOfService
import mqtt.wire.data.QualityOfService.AT_MOST_ONCE
import mqtt.wire4.control.packet.PublishMessage
import mqtt.wire4.control.packet.SubscribeRequest
import mqtt.wire4.control.packet.UnsubscribeRequest

interface IClient {
    var session: SocketSession?
    val persistence: Persistence
    val state: ClientSessionState

    fun connect(connectionParameters: ConnectionParameters)

    fun registerTopic(topicWildcard: List<String>, callback: SubscriptionCallback<Any>)
    suspend fun <T : Any> publish(topic: String, qos: QualityOfService, payload: T? = null)

    suspend fun <T> subscribe(topic: String, qos: QualityOfService, callback: SubscriptionCallback<T>) = subscribe(listOf(topic), listOf(qos), listOf(callback))
    suspend fun <T> subscribe(topics: List<String>, qos: List<QualityOfService>,
                              callback: List<SubscriptionCallback<T>>)

    suspend fun unsubscribe(topic: String) = unsubscribe(listOf(topic))
    suspend fun unsubscribe(topics: List<String>)

    fun disconnectAsync(): Deferred<Boolean>?
}

class Client(override val persistence: Persistence) : IClient {
    override var session: SocketSession? = null
    override val state = ClientSessionState()
    var disconnectReason: Deferred<AtomicRef<ConnectionState>>? = null

    override fun connect(connectionParameters: ConnectionParameters) {
        val session = PlatformSocketConnection(connectionParameters)
        this.session = session
        disconnectReason = session.openConnectionAsync(true)
    }

    override suspend fun <T : Any> publish(topic: String, qos: QualityOfService, payload: T?) = pub(topic, qos, payload)

    private suspend fun <T : Any> pub(topic: String, qos: QualityOfService, payload: T?) {
        val actualPayload = if (payload == null) {
            null
        } else {
            val serializer = findSerializer<T>()
                    ?: throw RuntimeException("Failed to find serializer for the class $payload")
            serializer.serialize(payload)
        }
        val publish = PublishMessage(topic, qos, actualPayload)
        val warning = publish.validateOrGetWarning()
        if (warning != null) {
            throw warning
        }
        if (qos == AT_MOST_ONCE) {
            val session = session ?: return
            session.clientToServer.send(publish)
        } else {
            // packet identifier has already been validated by the warning check above
            val oldPacket = persistence.put(publish.variable.packetIdentifier!!, publish)
            if (oldPacket != null) {
                throw IllegalStateException("Publishing a packet with a packet identifier that is still in the queue " +
                        "old:$oldPacket, new:$publish")
            }
        }
    }

    override fun registerTopic(topicWildcard: List<String>, callback: SubscriptionCallback<Any>) {

    }

    override suspend fun <T> subscribe(topics: List<String>, qos: List<QualityOfService>,
                                       callback: List<SubscriptionCallback<T>>) {
        val subscription = SubscribeRequest(topics, qos)
        val oldPacket = persistence.put(subscription.packetIdentifier, subscription)
        if (oldPacket != null) {
            throw IllegalStateException("Publishing a subscribe packet with a packet identifier that is still in " +
                    "the queue old:$oldPacket, new:$subscription")
        }
    }

    override suspend fun unsubscribe(topics: List<String>) {
        val unsubscribeRequest = UnsubscribeRequest(topics = topics.map { MqttUtf8String(it) })
        val oldPacket = persistence.put(unsubscribeRequest.packetIdentifier, unsubscribeRequest)
        if (oldPacket != null) {
            throw IllegalStateException("Publishing a unsubscribe packet with a packet identifier that is still in " +
                    "the queue old:$oldPacket, new:$unsubscribeRequest")
        }
    }

    override fun disconnectAsync(): Deferred<Boolean>? {
        val result = session?.closeAsync()
        session = null
        state.qos1And2MessagesSentButNotAcked.clear()
        state.qos2MessagesRecevedBytNotCompletelyAcked.clear()
        return result
    }

}
