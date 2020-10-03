@file:Suppress("EXPERIMENTAL_API_USAGE", "EXPERIMENTAL_UNSIGNED_LITERALS")

package mqtt.wire5.control.packet

import mqtt.buffer.allocateNewBuffer
import mqtt.wire.ProtocolError
import mqtt.wire.control.packet.format.ReasonCode
import mqtt.wire.control.packet.format.ReasonCode.RECEIVE_MAXIMUM_EXCEEDED
import mqtt.wire5.control.packet.PublishRelease.VariableHeader
import mqtt.wire5.control.packet.format.variable.property.ReasonString
import mqtt.wire5.control.packet.format.variable.property.UserProperty
import mqtt.wire5.control.packet.format.variable.property.readProperties
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.fail

class PublishReleaseTests {
    private val packetIdentifier = 2

    @Test
    fun packetIdentifier() {
        val pubrel = PublishRelease(VariableHeader(packetIdentifier))
        val buffer = allocateNewBuffer(4u, limits)
        pubrel.serialize(buffer)
        buffer.resetForRead()
        assertEquals(0b01100010, buffer.readByte(), "fixed header byte1")
        assertEquals(2u, buffer.readVariableByteInteger(), "fixed header byte2 remaining length")
        assertEquals(packetIdentifier, buffer.readUnsignedShort().toInt(), "variable header byte 1-2")
        buffer.resetForRead()
        val pubrelResult = ControlPacketV5.from(buffer) as PublishRelease
        assertEquals(pubrelResult.variable.packetIdentifier, packetIdentifier)
    }

    @Test
    fun defaultAndNonDefaultSuccessDeserialization() {
        val pubrel = PublishRelease(VariableHeader(packetIdentifier))
        val bufferNonDefaults = allocateNewBuffer(6u, limits)
        bufferNonDefaults.write(0b01100010.toByte())
        bufferNonDefaults.writeVariableByteInteger(4u)
        bufferNonDefaults.write(packetIdentifier.toUShort())
        bufferNonDefaults.write(0.toUByte())
        bufferNonDefaults.writeVariableByteInteger(0u)
        bufferNonDefaults.resetForRead()
        val pubrelResult = ControlPacketV5.from(bufferNonDefaults) as PublishRelease
        assertEquals(pubrel, pubrelResult)

    }

    @Test
    fun invalidReasonCodeThrowsProtocolError() {
        try {
            PublishRelease(VariableHeader(packetIdentifier, RECEIVE_MAXIMUM_EXCEEDED))
            fail()
        } catch (e: ProtocolError) {
        }
    }


    @Test
    fun reasonString() {
        val expected = PublishRelease(
            VariableHeader(
                packetIdentifier,
                properties = VariableHeader.Properties(reasonString = "yolo".toCharSequenceBuffer())
            )
        )
        val buffer = allocateNewBuffer(13u, limits)
        expected.serialize(buffer)
        buffer.resetForRead()
        assertEquals(0b01100010, buffer.readByte(), "fixed header byte1")
        assertEquals(11u, buffer.readVariableByteInteger(), "fixed header byte2 remaining length")
        assertEquals(packetIdentifier, buffer.readUnsignedShort().toInt(), "variable header byte 1-2")
        assertEquals(ReasonCode.SUCCESS.byte, buffer.readUnsignedByte(), "reason code")
        assertEquals(7u, buffer.readVariableByteInteger(), "property length")
        assertEquals(0x1F, buffer.readByte(), "user property identifier")
        assertEquals("yolo", buffer.readMqttUtf8StringNotValidated().toString(), "reason string")
        buffer.resetForRead()
        val pubrelResult = ControlPacketV5.from(buffer) as PublishRelease
        assertEquals(expected.variable.properties.reasonString.toString(), "yolo")
        assertEquals(expected, pubrelResult)
    }

    @Test
    fun reasonStringMultipleTimesThrowsProtocolError() {
        val obj1 = ReasonString("yolo")
        val obj2 = obj1.copy()
        val buffer = allocateNewBuffer(15u, limits)
        buffer.writeVariableByteInteger(obj1.size() + obj2.size())
        obj1.write(buffer)
        obj2.write(buffer)
        buffer.resetForRead()
        assertFailsWith<ProtocolError> { VariableHeader.Properties.from(buffer.readProperties()) }
    }


    @Test
    fun variableHeaderPropertyUserProperty() {
        val props = VariableHeader.Properties.from(setOf(UserProperty("key", "value")))
        val userPropertyResult = props.userProperty
        for ((key, value) in userPropertyResult) {
            assertEquals(key, "key")
            assertEquals(value, "value")
        }
        assertEquals(userPropertyResult.size, 1)

        val buffer = allocateNewBuffer(19u, limits)
        val request = PublishRelease(VariableHeader(packetIdentifier, properties = props))
        request.serialize(buffer)
        buffer.resetForRead()
        val requestRead = ControlPacketV5.from(buffer) as PublishRelease
        val (key, value) = requestRead.variable.properties.userProperty.first()
        assertEquals(key.toString(), "key")
        assertEquals(value.toString(), "value")
    }

}
