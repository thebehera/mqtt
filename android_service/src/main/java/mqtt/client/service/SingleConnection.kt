package mqtt.client.service

import mqtt.Log
import mqtt.client.MqttClient
import mqtt.client.connection.ConnectionParameters
import mqtt.wire.data.QualityOfService
import mqtt.wire.data.topic.Name

class SingleConnection(
    val params: ConnectionParameters,
    val log: Log
) {

    val client = MqttClient(params)

    fun connect() = client.connectAsync()

    suspend inline fun <reified T : Any> publish(topic: String, qos: QualityOfService, obj: T) =
        client.publish(topic, qos, obj)

    suspend inline fun <reified T : Any> subscribe(
        topicFilter: String, qos: QualityOfService,
        crossinline callback: (topic: Name, qos: QualityOfService, message: T?) -> Unit
    ) = client.subscribe(topicFilter, qos, callback)

    fun disconnect() = client.disconnectAsync()
}