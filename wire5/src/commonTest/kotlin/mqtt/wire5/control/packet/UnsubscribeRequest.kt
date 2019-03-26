@file:Suppress("EXPERIMENTAL_API_USAGE")

package mqtt.wire5.control.packet

import mqtt.wire.data.MqttUtf8String
import mqtt.wire5.control.packet.UnsubscribeRequest.VariableHeader
import mqtt.wire5.control.packet.format.variable.property.UserProperty
import kotlin.test.Test
import kotlin.test.assertEquals

class UnsubscribeRequestTests {

    private val packetIdentifier = 2.toUShort()

    @Test
    fun basicTest() {
        val unsub = UnsubscribeRequest(VariableHeader(packetIdentifier), setOf(MqttUtf8String("yolo"), MqttUtf8String("yolo")))
        val result = ControlPacketV5.from(unsub.serialize()) as UnsubscribeRequest
        assertEquals(result.topics.first().getValueOrThrow(), "yolo")
    }

    @Test
    fun variableHeaderPropertyUserProperty() {
        val props = VariableHeader.Properties.from(setOf(UserProperty(MqttUtf8String("key"), MqttUtf8String("value"))))
        val userPropertyResult = props.userProperty
        for ((key, value) in userPropertyResult) {
            assertEquals(key.getValueOrThrow(), "key")
            assertEquals(value.getValueOrThrow(), "value")
        }
        assertEquals(userPropertyResult.size, 1)

        val request = UnsubscribeRequest(VariableHeader(packetIdentifier, properties = props), setOf(MqttUtf8String("test"))).serialize()
        val requestRead = ControlPacketV5.from(request.copy()) as UnsubscribeRequest
        val (key, value) = requestRead.variable.properties.userProperty.first()
        assertEquals(key.getValueOrThrow(), "key")
        assertEquals(value.getValueOrThrow(), "value")
    }
}
