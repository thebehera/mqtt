@file:Suppress("EXPERIMENTAL_API_USAGE", "EXPERIMENTAL_UNSIGNED_LITERALS")

package mqtt.wire5.control.packet

import kotlinx.io.core.*
import mqtt.buffer.allocateNewBuffer
import mqtt.wire.ProtocolError
import mqtt.wire.data.MqttUtf8String
import mqtt.wire.data.QualityOfService.AT_LEAST_ONCE
import mqtt.wire.data.QualityOfService.EXACTLY_ONCE
import mqtt.wire.data.VariableByteInteger
import mqtt.wire.data.topic.Filter
import mqtt.wire5.control.packet.SubscribeRequest.VariableHeader
import mqtt.wire5.control.packet.format.variable.property.ReasonString
import mqtt.wire5.control.packet.format.variable.property.UserProperty
import mqtt.wire5.control.packet.format.variable.property.readPropertiesLegacy
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.fail

class SubscribeRequestTest {
    private val packetIdentifier = 2.toUShort()
    @Test
    fun simpleTest() {
        val subscribeRequest = SubscribeRequest(2.toUShort(), "test", AT_LEAST_ONCE)
        assertEquals(subscribeRequest.variable.packetIdentifier, 2)
        assertEquals(subscribeRequest.subscriptions.first().topicFilter.validate().toString(), "test")
        val buffer = allocateNewBuffer(12u, limits)
        subscribeRequest.serialize(buffer)
        println(buffer)
        buffer.resetForRead()
        // fixed header 2 bytes
        // byte 1 fixed header
        assertEquals(0b10000010.toUByte(), buffer.readUnsignedByte())
        // byte 2 fixed header
        assertEquals(10u, buffer.readVariableByteInteger())

        // Variable header 3 bytes
        // byte 1 & 2 variable header as Ushort for packet identifier
        assertEquals(packetIdentifier.toUShort(), buffer.readUnsignedShort())

        // byte 3 variable header, property length
        assertEquals(0.toUByte(), buffer.readUnsignedByte())

        // Payload 12 bytes
        // Topic Filter ("a/b")
        // byte 1: Length MSB (0)
        assertEquals(0b00000000, buffer.readByte())
        // byte2: Length LSB (4)
        assertEquals(4, buffer.readByte())
        // byte3: t
        assertEquals('t', buffer.readByte().toChar())
        // byte4: / (0x2F)
        assertEquals('e', buffer.readByte().toChar())
        // byte5: b (0x62)
        assertEquals('s', buffer.readByte().toChar())
        // byte6: b (0x62)
        assertEquals('t', buffer.readByte().toChar())
        // Subscription Options
        // byte7: Subscription Options (1)
        assertEquals(0b00000001, buffer.readByte())
        println(buffer)
        buffer.resetForRead()
        val requestRead = ControlPacketV5.from(buffer) as SubscribeRequest
        assertEquals(requestRead.subscriptions.first().topicFilter.validate().toString(), "test")
        assertEquals(AT_LEAST_ONCE, requestRead.subscriptions.first().maximumQos)
    }

    @Test
    fun serialized() {
        val subscribeRequest = SubscribeRequest(2.toUShort(), listOf("a/b", "c/d"), listOf(AT_LEAST_ONCE, EXACTLY_ONCE))
        assertEquals(subscribeRequest.variable.packetIdentifier, 2)
        val buffer = allocateNewBuffer(17u, limits)
        subscribeRequest.serialize(buffer)
        buffer.resetForRead()
        // fixed header 2 bytes
        // byte 1 fixed header
        assertEquals(0b10000010.toUByte(), buffer.readUnsignedByte())
        // byte 2 fixed header
        assertEquals(15u, buffer.readVariableByteInteger())

        // Variable header 3 bytes
        // byte 1 & 2 variable header as Ushort for packet identifier
        assertEquals(packetIdentifier.toUShort(), buffer.readUnsignedShort())

        // byte 3 variable header, property length
        assertEquals(0.toUByte(), buffer.readUnsignedByte())

        // Payload 12 bytes
        // Topic Filter ("a/b")
        // byte 1: Length MSB (0)
        assertEquals(0b00000000, buffer.readByte())
        // byte2: Length LSB (3)
        assertEquals(0b00000011, buffer.readByte())
        // byte3: a (0x61)
        assertEquals(0b01100001, buffer.readByte())
        // byte4: / (0x2F)
        assertEquals(0b00101111, buffer.readByte())
        // byte5: b (0x62)
        assertEquals(0b01100010, buffer.readByte())
        // Subscription Options
        // byte6: Subscription Options (1)
        assertEquals(0b00000001, buffer.readByte())


        // Topic Filter ("c/d")
        // byte 1: Length MSB (0)
        assertEquals(0b00000000, buffer.readByte())
        // byte2: Length LSB (3)
        assertEquals(0b00000011, buffer.readByte())
        // byte3: c (0x63)
        assertEquals(0b01100011, buffer.readByte())
        // byte4: / (0x2F)
        assertEquals(0b00101111, buffer.readByte())
        // byte5: d (0x64)
        assertEquals(0b01100100, buffer.readByte())
        // Subscription Options
        // byte6: Subscription Options (2)
        assertEquals(0b00000010, buffer.readByte())
    }

    @Test
    fun subscriptionPayloadOptions() {
        val subscription = Subscription.from("a/b", AT_LEAST_ONCE)
        val buffer = subscription.packet
        assertEquals(3.toUShort(), buffer.readUShort())
        assertEquals("a/b", buffer.readTextExact(3))
        assertEquals(0b000001, buffer.readByte())
    }

    @Test
    fun subscriptionPayloadSize() {
        val request = SubscribeRequest(0.toUShort(), listOf("a/b", "c/d"),
                listOf(AT_LEAST_ONCE, EXACTLY_ONCE))
        assertEquals(12.toUInt(), request.payloadPacketSize)
    }

    @Test
    fun subscriptionPayload() {
        val request = SubscribeRequest(0.toUShort(), listOf("a/b", "c/d"), listOf(AT_LEAST_ONCE, EXACTLY_ONCE))
        val buffer = request.payloadPacket()
        // Topic Filter ("a/b")
        // byte 1: Length MSB (0)
        assertEquals(0b00000000, buffer.readByte())
        // byte2: Length LSB (3)
        assertEquals(0b00000011, buffer.readByte())
        // byte3: a (0x61)
        assertEquals(0b01100001, buffer.readByte())
        // byte4: / (0x2F)
        assertEquals(0b00101111, buffer.readByte())
        // byte5: b (0x62)
        assertEquals(0b01100010, buffer.readByte())
        // Subscription Options
        // byte6: Subscription Options (1)
        assertEquals(0b00000001, buffer.readByte())


        // Topic Filter ("c/d")
        // byte 1: Length MSB (0)
        assertEquals(0b00000000, buffer.readByte())
        // byte2: Length LSB (3)
        assertEquals(0b00000011, buffer.readByte())
        // byte3: c (0x63)
        assertEquals(0b01100011, buffer.readByte())
        // byte4: / (0x2F)
        assertEquals(0b00101111, buffer.readByte())
        // byte5: d (0x64)
        assertEquals(0b01100100, buffer.readByte())
        // Subscription Options
        // byte6: Subscription Options (2)
        assertEquals(0b00000010, buffer.readByte())
        // No more bytes to read
        assertEquals(0, buffer.remaining)
    }

    @Test
    fun reasonString() {
        val actual = SubscribeRequest(VariableHeader(packetIdentifier.toInt(), properties = VariableHeader.Properties(reasonString = MqttUtf8String("yolo"))), setOf(Subscription(Filter("test"))))
        val bytes = actual.serialize()
        val expected = ControlPacketV5.from(bytes) as SubscribeRequest
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
            VariableHeader.Properties.from(props.readPropertiesLegacy())
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

        val request = SubscribeRequest(VariableHeader(packetIdentifier.toInt(), properties = props), setOf(Subscription(Filter("test")))).serialize()
        val requestRead = ControlPacketV5.from(request.copy()) as SubscribeRequest
        val (key, value) = requestRead.variable.properties.userProperty.first()
        assertEquals(key.getValueOrThrow(), "key")
        assertEquals(value.getValueOrThrow(), "value")
    }


}
