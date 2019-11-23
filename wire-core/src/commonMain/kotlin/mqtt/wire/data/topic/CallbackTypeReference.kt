package mqtt.wire.data.topic

import mqtt.wire.control.packet.IPublishMessage
import mqtt.wire.control.packet.MqttSerializable
import mqtt.wire.control.packet.findSerializer
import kotlin.reflect.KClass

data class CallbackTypeReference<T : Any>
constructor(
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
}
