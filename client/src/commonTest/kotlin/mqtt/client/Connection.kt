@file:Suppress("EXPERIMENTAL_API_USAGE")

package mqtt.client

import mqtt.wire.data.MqttUtf8String
import mqtt.wire.data.QualityOfService
import mqtt.wire4.control.packet.ConnectionRequest
import mqtt.wire4.control.packet.DisconnectNotification
import mqtt.wire4.control.packet.PublishMessage
import kotlin.test.Test

class ConnectionTests {
    @Test
    fun connectAndDisconnect() {
        val header = ConnectionRequest.VariableHeader(keepAliveSeconds = 5.toUShort())
        val payload = ConnectionRequest.Payload(clientId = MqttUtf8String("JavaSample"))
        val params = ConnectionParameters("test.mosquitto.org", 1883, false,
                ConnectionRequest(header, payload), reconnectIfNetworkLost = false)
        val connection = openConnection(params)
        block {
            val fixed = PublishMessage.FixedHeader(qos = QualityOfService.AT_LEAST_ONCE)
            val variable = PublishMessage.VariableHeader(MqttUtf8String("yolo"), 1.toUShort())
            params.clientToBroker.send(PublishMessage(fixed, variable))
            params.clientToBroker.send(DisconnectNotification)
            connection.await()
        }
    }
}
