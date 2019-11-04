package mqtt.wire.data.topic

import mqtt.wire.control.packet.IPublishMessage
import mqtt.wire.control.packet.MqttSerializable
import mqtt.wire.control.packet.findSerializer
import mqtt.wire.data.QualityOfService
import kotlin.reflect.KClass

data class CallbackTypeReference<T : Any>(
    val callback: SubscriptionCallback<T>,
    val klass: KClass<T>,
    val serializer: MqttSerializable<T>? = findSerializer(klass)
) {
    fun handleCallback(incomingPublish: IPublishMessage) {
        val payload = incomingPublish.payloadPacket()
        val topic = incomingPublish.topic
        val qos = incomingPublish.qualityOfService
        if (payload == null) {
            callback.onMessageReceived(topic, qos, null)
            return
        }
        if (serializer == null) {
            println("Failed to find serializer for payload $callback")
            callback.onMessageReceived(topic, qos, null)
            return
        }
        val message = serializer.deserialize(payload)
        callback.onMessageReceived(topic, qos, message)
    }

    fun handleMessage(topic: Name, qos: QualityOfService, msg: Any) {
        if (klass.isInstance(msg)) {
            @Suppress("UNCHECKED_CAST")
            callback.onMessageReceived(topic, qos, msg as T)
        }
    }
}
