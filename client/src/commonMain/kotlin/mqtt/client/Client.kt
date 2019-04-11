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

class Client(val persistence: Persistence) {
    var session: SocketSession? = null
    val state = ClientSessionState()
    var disconnectReason: Deferred<AtomicRef<ConnectionState>>? = null
    val subscriptionManager = SubscriptionManager()

    fun connect(connectionParameters: ConnectionParameters) {
        val session = PlatformSocketConnection(connectionParameters)
        this.session = session
        disconnectReason = session.openConnectionAsync(true)
    }

    suspend inline fun <reified T : Any> publish(topic: String, qos: QualityOfService, payload: T?) {
        pub(topic, qos, payload)
    }

    suspend inline fun <reified T : Any> pub(topic: String, qos: QualityOfService, payload: T?) {
        val actualPayload = if (payload == null) {
            null
        } else {
            val serializer = findSerializer<T>()
                    ?: throw RuntimeException("Failed to find serializer for $payload")
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


    fun <T> subscribe(topics: List<String>, qos: List<QualityOfService>,
                      callback: List<SubscriptionCallback<T>>) {
        val subscription = SubscribeRequest(topics, qos)
        val oldPacket = persistence.put(subscription.packetIdentifier, subscription)
        if (oldPacket != null) {
            throw IllegalStateException("Publishing a subscribe packet with a packet identifier that is still in " +
                    "the queue old:$oldPacket, new:$subscription")
        }
    }

    fun unsubscribe(topics: List<String>) {
        val unsubscribeRequest = UnsubscribeRequest(topics = topics.map { MqttUtf8String(it) })
        val oldPacket = persistence.put(unsubscribeRequest.packetIdentifier, unsubscribeRequest)
        if (oldPacket != null) {
            throw IllegalStateException("Publishing a unsubscribe packet with a packet identifier that is still in " +
                    "the queue old:$oldPacket, new:$unsubscribeRequest")
        }
    }

    fun disconnectAsync(): Deferred<Boolean>? {
        val result = session?.closeAsync()
        session = null
        return result
    }

}
