@file:Suppress("EXPERIMENTAL_API_USAGE")

package mqtt.wire5.control.packet

import kotlinx.io.core.buildPacket
import kotlinx.io.core.readBytes
import kotlinx.io.core.writeFully
import mqtt.wire.ProtocolError
import mqtt.wire.data.MqttUtf8String
import mqtt.wire.data.VariableByteInteger
import mqtt.wire5.control.packet.SubscribeRequest.VariableHeader
import mqtt.wire5.control.packet.format.variable.property.ReasonString
import mqtt.wire5.control.packet.format.variable.property.UserProperty
import mqtt.wire5.control.packet.format.variable.property.readProperties
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.fail

class SubscribeRequestTest {
    private val packetIdentifier = 2.toUShort()
    @Test
    fun simpleTest() {
        val subscribeRequest = SubscribeRequest(
                VariableHeader(2.toUShort()), setOf(Subscription(MqttUtf8String("test"))))
        assertEquals(subscribeRequest.variable.packetIdentifier, 2.toUShort())
        assertEquals(subscribeRequest.subscriptions.first().topicFilter.getValueOrThrow(), "test")
        val subscribeRequestData = subscribeRequest.serialize()
        val requestRead = ControlPacket.from(subscribeRequestData) as SubscribeRequest
        assertEquals(requestRead.subscriptions.first().topicFilter.getValueOrThrow(), "test")
    }

    @Test
    fun reasonString() {
        val actual = SubscribeRequest(VariableHeader(packetIdentifier, properties = VariableHeader.Properties(reasonString = MqttUtf8String("yolo"))), setOf(Subscription(MqttUtf8String("test"))))
        val bytes = actual.serialize()
        val expected = ControlPacket.from(bytes) as SubscribeRequest
        assertEquals(expected.variable.properties.reasonString, MqttUtf8String("yolo"))
    }

    @Test
    fun reasonStringMultipleTimesThrowsProtocolError() {
        val obj1 = ReasonString(MqttUtf8String("yolo"))
        val obj2 = obj1.copy()
        val propsWithoutPropertyLength = buildPacket {
            obj1.write(this)
            obj2.write(this)
        }.readBytes()
        val props = buildPacket {
            writePacket(VariableByteInteger(propsWithoutPropertyLength.size.toUInt()).encodedValue())
            writeFully(propsWithoutPropertyLength)
        }.copy()
        try {
            VariableHeader.Properties.from(props.readProperties())
            fail()
        } catch (e: ProtocolError) {
        }
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

        val request = SubscribeRequest(VariableHeader(packetIdentifier, properties = props), setOf(Subscription(MqttUtf8String("test")))).serialize()
        val requestRead = ControlPacket.from(request.copy()) as SubscribeRequest
        val (key, value) = requestRead.variable.properties.userProperty.first()
        assertEquals(key.getValueOrThrow(), "key")
        assertEquals(value.getValueOrThrow(), "value")
    }
}
