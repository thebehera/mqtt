@file:Suppress("EXPERIMENTAL_API_USAGE", "EXPERIMENTAL_UNSIGNED_LITERALS")

package mqtt.wire5.control.packet


import mqtt.buffer.allocateNewBuffer
import mqtt.wire.MalformedPacketException
import mqtt.wire.ProtocolError
import mqtt.wire.buffer.readMqttUtf8StringNotValidated
import mqtt.wire.buffer.readVariableByteInteger
import mqtt.wire.buffer.writeVariableByteInteger
import mqtt.wire.control.packet.format.ReasonCode.*
import mqtt.wire5.control.packet.UnsubscribeAcknowledgment.VariableHeader
import mqtt.wire5.control.packet.format.variable.property.ReasonString
import mqtt.wire5.control.packet.format.variable.property.UserProperty
import mqtt.wire5.control.packet.format.variable.property.readProperties
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.fail

class UnsubscribeAcknowledgmentTests {
    private val packetIdentifier = 2

    @Test
    fun serializeDeserializeDefault() {
        val actual = UnsubscribeAcknowledgment(VariableHeader(packetIdentifier))
        val buffer = allocateNewBuffer(6u)
        actual.serialize(buffer)
        buffer.resetForRead()
        assertEquals(0b10110000.toByte(), buffer.readByte(), "fixed header byte 1")
        assertEquals(4u, buffer.readVariableByteInteger(), "fixed header byte 2 remaining length")
        assertEquals(0, buffer.readByte(), "variable header byte 1 packet identifier msb")
        assertEquals(2, buffer.readByte(), "variable header byte 2 packet identifier lsb")
        assertEquals(0u, buffer.readVariableByteInteger(), "property length")
        assertEquals(SUCCESS.byte, buffer.readUnsignedByte(), "payload reason code")
        buffer.resetForRead()
        val expected = ControlPacketV5.from(buffer)
        assertEquals(expected, actual)
    }

    @Test
    fun serializeDeserializeNoSubscriptionsExisted() {
        val actual = UnsubscribeAcknowledgment(VariableHeader(packetIdentifier), listOf(NO_SUBSCRIPTIONS_EXISTED))
        val buffer = allocateNewBuffer(6u)
        actual.serialize(buffer)
        buffer.resetForRead()
        assertEquals(0b10110000.toByte(), buffer.readByte(), "fixed header byte 1")
        assertEquals(4u, buffer.readVariableByteInteger(), "fixed header byte 2 remaining length")
        assertEquals(0, buffer.readByte(), "variable header byte 1 packet identifier msb")
        assertEquals(2, buffer.readByte(), "variable header byte 2 packet identifier lsb")
        assertEquals(0u, buffer.readVariableByteInteger(), "property length")
        assertEquals(NO_SUBSCRIPTIONS_EXISTED.byte, buffer.readUnsignedByte(), "payload reason code")
        buffer.resetForRead()
        val expected = ControlPacketV5.from(buffer)
        assertEquals(expected, actual)
    }

    @Test
    fun serializeDeserializeUnspecifiedError() {
        val actual = UnsubscribeAcknowledgment(VariableHeader(packetIdentifier), listOf(UNSPECIFIED_ERROR))
        val buffer = allocateNewBuffer(6u)
        actual.serialize(buffer)
        buffer.resetForRead()
        assertEquals(0b10110000.toByte(), buffer.readByte(), "fixed header byte 1")
        assertEquals(4u, buffer.readVariableByteInteger(), "fixed header byte 2 remaining length")
        assertEquals(0, buffer.readByte(), "variable header byte 1 packet identifier msb")
        assertEquals(2, buffer.readByte(), "variable header byte 2 packet identifier lsb")
        assertEquals(0u, buffer.readVariableByteInteger(), "property length")
        assertEquals(UNSPECIFIED_ERROR.byte, buffer.readUnsignedByte(), "payload reason code")
        buffer.resetForRead()
        val expected = ControlPacketV5.from(buffer)
        assertEquals(expected, actual)
    }

    @Test
    fun serializeDeserializeImplementationSpecificError() {
        val actual = UnsubscribeAcknowledgment(VariableHeader(packetIdentifier), listOf(IMPLEMENTATION_SPECIFIC_ERROR))
        val buffer = allocateNewBuffer(6u)
        actual.serialize(buffer)
        buffer.resetForRead()
        assertEquals(0b10110000.toByte(), buffer.readByte(), "fixed header byte 1")
        assertEquals(4u, buffer.readVariableByteInteger(), "fixed header byte 2 remaining length")
        assertEquals(0, buffer.readByte(), "variable header byte 1 packet identifier msb")
        assertEquals(2, buffer.readByte(), "variable header byte 2 packet identifier lsb")
        assertEquals(0u, buffer.readVariableByteInteger(), "property length")
        assertEquals(IMPLEMENTATION_SPECIFIC_ERROR.byte, buffer.readUnsignedByte(), "payload reason code")
        buffer.resetForRead()
        val expected = ControlPacketV5.from(buffer)
        assertEquals(expected, actual)
    }

    @Test
    fun serializeDeserializeNotAuthorized() {
        val actual = UnsubscribeAcknowledgment(VariableHeader(packetIdentifier), listOf(NOT_AUTHORIZED))
        val buffer = allocateNewBuffer(6u)
        actual.serialize(buffer)
        buffer.resetForRead()
        assertEquals(0b10110000.toByte(), buffer.readByte(), "fixed header byte 1")
        assertEquals(4u, buffer.readVariableByteInteger(), "fixed header byte 2 remaining length")
        assertEquals(0, buffer.readByte(), "variable header byte 1 packet identifier msb")
        assertEquals(2, buffer.readByte(), "variable header byte 2 packet identifier lsb")
        assertEquals(0u, buffer.readVariableByteInteger(), "property length")
        assertEquals(NOT_AUTHORIZED.byte, buffer.readUnsignedByte(), "payload reason code")
        buffer.resetForRead()
        val expected = ControlPacketV5.from(buffer)
        assertEquals(expected, actual)
    }

    @Test
    fun serializeDeserializeTopicFilterInvalid() {
        val actual = UnsubscribeAcknowledgment(VariableHeader(packetIdentifier), listOf(TOPIC_FILTER_INVALID))
        val buffer = allocateNewBuffer(6u)
        actual.serialize(buffer)
        buffer.resetForRead()
        assertEquals(0b10110000.toByte(), buffer.readByte(), "fixed header byte 1")
        assertEquals(4u, buffer.readVariableByteInteger(), "fixed header byte 2 remaining length")
        assertEquals(0, buffer.readByte(), "variable header byte 1 packet identifier msb")
        assertEquals(2, buffer.readByte(), "variable header byte 2 packet identifier lsb")
        assertEquals(0u, buffer.readVariableByteInteger(), "property length")
        assertEquals(TOPIC_FILTER_INVALID.byte, buffer.readUnsignedByte(), "payload reason code")
        buffer.resetForRead()
        val expected = ControlPacketV5.from(buffer)
        assertEquals(expected, actual)
    }

    @Test
    fun emptyReasonCodesThrowsProtocolError() {
        try {
            UnsubscribeAcknowledgment(VariableHeader(packetIdentifier), listOf())
            fail()
        } catch (e: ProtocolError) {
        }
    }

    @Test
    fun serializeDeserializePacketIdentifierInUse() {
        val actual = UnsubscribeAcknowledgment(VariableHeader(packetIdentifier), listOf(PACKET_IDENTIFIER_IN_USE))
        val buffer = allocateNewBuffer(6u)
        actual.serialize(buffer)
        buffer.resetForRead()
        assertEquals(0b10110000.toByte(), buffer.readByte(), "fixed header byte 1")
        assertEquals(4u, buffer.readVariableByteInteger(), "fixed header byte 2 remaining length")
        assertEquals(0, buffer.readByte(), "variable header byte 1 packet identifier msb")
        assertEquals(2, buffer.readByte(), "variable header byte 2 packet identifier lsb")
        assertEquals(0u, buffer.readVariableByteInteger(), "property length")
        assertEquals(PACKET_IDENTIFIER_IN_USE.byte, buffer.readUnsignedByte(), "payload reason code")
        buffer.resetForRead()
        val expected = ControlPacketV5.from(buffer)
        assertEquals(expected, actual)
    }

    @Test
    fun reasonString() {
        val props = VariableHeader.Properties(reasonString = "yolo")
        val header = VariableHeader(packetIdentifier, properties = props)
        val actual = UnsubscribeAcknowledgment(header)
        val buffer = allocateNewBuffer(13u)
        actual.serialize(buffer)
        buffer.resetForRead()
        assertEquals(0b10110000.toByte(), buffer.readByte(), "fixed header byte 1")
        assertEquals(11u, buffer.readVariableByteInteger(), "fixed header byte 2 remaining length")
        assertEquals(0, buffer.readByte(), "variable header byte 1 packet identifier msb")
        assertEquals(2, buffer.readByte(), "variable header byte 2 packet identifier lsb")
        assertEquals(7u, buffer.readVariableByteInteger(), "property length")
        assertEquals(0x1F, buffer.readByte(), "property type matching reason code")
        assertEquals("yolo", buffer.readMqttUtf8StringNotValidated().toString(), "reason code value")
        assertEquals(SUCCESS.byte, buffer.readUnsignedByte(), "payload reason code")
        buffer.resetForRead()
        val expected = ControlPacketV5.from(buffer) as UnsubscribeAcknowledgment
        assertEquals(expected.variable.properties.reasonString.toString(), "yolo")
    }

    @Test
    fun reasonStringMultipleTimesThrowsProtocolError() {
        val obj1 = ReasonString("yolo")
        val obj2 = obj1.copy()
        val buffer = allocateNewBuffer(15u)
        buffer.writeVariableByteInteger(obj1.size() + obj2.size())
        obj1.write(buffer)
        obj2.write(buffer)
        buffer.resetForRead()
        assertFailsWith<ProtocolError> { VariableHeader.Properties.from(buffer.readProperties()) }
    }

    @Test
    fun variableHeaderPropertyUserProperty() {
        val props = VariableHeader.Properties.from(
            setOf(
                UserProperty("key", "value"),
                UserProperty("key", "value")
            )
        )
        val userPropertyResult = props.userProperty
        for ((key, value) in userPropertyResult) {
            assertEquals(key, "key")
            assertEquals(value, "value")
        }
        assertEquals(userPropertyResult.size, 1)

        val request = UnsubscribeAcknowledgment(VariableHeader(packetIdentifier, properties = props))
        val buffer = allocateNewBuffer(19u)
        request.serialize(buffer)
        buffer.resetForRead()
        val requestRead = ControlPacketV5.from(buffer) as UnsubscribeAcknowledgment
        val (key, value) = requestRead.variable.properties.userProperty.first()
        assertEquals("key", key.toString())
        assertEquals("value", value.toString())
    }

    @Test
    fun invalidReasonCode() {
        val variable = VariableHeader(packetIdentifier)
        val buffer = allocateNewBuffer(4u)
        variable.serialize(buffer)
        buffer.write(BANNED.byte)
        buffer.resetForRead()
        assertFailsWith<MalformedPacketException> { UnsubscribeAcknowledgment.from(buffer, 2u) }
    }
}
