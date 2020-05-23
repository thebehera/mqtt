@file:Suppress("EXPERIMENTAL_API_USAGE", "EXPERIMENTAL_UNSIGNED_LITERALS")

package mqtt.wire4.control.packet

import mqtt.buffer.allocateNewBuffer
import mqtt.wire.MalformedPacketException
import mqtt.wire.control.packet.format.fixed.get
import mqtt.wire.data.QualityOfService
import mqtt.wire4.control.packet.PublishMessage.FixedHeader
import mqtt.wire4.control.packet.PublishMessage.VariableHeader
import kotlin.test.*

class PublishMessageTests {

    @Test
    fun qosBothBitsSetTo1ThrowsMalformedPacketException() {
        val byte1 = 0b00111110.toByte()
        val remainingLength = 1.toByte()
        val buffer = allocateNewBuffer(2u, limits)
        buffer.write(byte1)
        buffer.write(remainingLength)
        buffer.resetForRead()
        try {
            ControlPacketV4.from(buffer)
            fail()
        } catch (e: MalformedPacketException) {
        }
    }

    @Test
    fun qos0AndPacketIdentifierThrowsIllegalArgumentException() {
        val fixed = FixedHeader(qos = QualityOfService.AT_MOST_ONCE)
        val variable = VariableHeader(("t"), 2)
        try {
            PublishMessage<Unit>(fixed, variable)
            fail()
        } catch (e: IllegalArgumentException) {
        }
    }

    @Test
    fun qos1WithoutPacketIdentifierThrowsIllegalArgumentException() {
        val fixed = FixedHeader(qos = QualityOfService.AT_LEAST_ONCE)
        val variable = VariableHeader(("t"))
        try {
            PublishMessage<Unit>(fixed, variable)
            fail()
        } catch (e: IllegalArgumentException) {
        }
    }

    @Test
    fun qos2WithoutPacketIdentifierThrowsIllegalArgumentException() {
        val fixed = FixedHeader(qos = QualityOfService.EXACTLY_ONCE)
        val variable = VariableHeader(("t"))
        try {
            PublishMessage<Unit>(fixed, variable)
            fail()
        } catch (e: IllegalArgumentException) {
        }
    }

    @Test
    fun genericSerialization() {
        val publishMessage = PublishMessage.buildTyped(topicName = "user/log", payload = "yolo")
        val buffer = allocateNewBuffer(16u)
        publishMessage.serialize(buffer)
        buffer.resetForRead()
        val firstByte = buffer.readUnsignedByte()
        assertEquals(firstByte.toInt().shr(4), 3, "fixed header control packet type")
        assertFalse(firstByte.get(3), "fixed header publish dup flag")
        assertFalse(firstByte.get(2), "fixed header qos bit 2")
        assertFalse(firstByte.get(1), "fixed header qos bit 1")
        assertFalse(firstByte.get(0), "fixed header retain flag")
        assertEquals(buffer.readVariableByteInteger(), 14u, "fixed header remaining length")
        assertEquals(8u, buffer.readUnsignedShort(), "variable header topic name length")
        assertEquals("user/log", buffer.readUtf8(8u).toString(), "variable header topic name value")
        if (publishMessage.variable.packetIdentifier != null) {
            assertEquals(buffer.readUnsignedShort().toInt(), publishMessage.variable.packetIdentifier)
        }
        assertEquals("yolo", buffer.readUtf8(4u).toString(), "payload value")
        buffer.resetForRead()
        val byte1 = buffer.readUnsignedByte()
        val remainingLength = buffer.readVariableByteInteger()
        val result = PublishMessage.from<String>(buffer, byte1, remainingLength)
        assertEquals(publishMessage, result)
    }

    @Test
    fun genericSerializationPublishDupFlag() {
        val publishMessage = PublishMessage.buildTyped(topicName = "user/log", payload = "yolo", dup = true)
        val buffer = allocateNewBuffer(16u)
        publishMessage.serialize(buffer)
        buffer.resetForRead()
        val firstByte = buffer.readUnsignedByte()
        assertEquals(firstByte.toInt().shr(4), 3, "fixed header control packet type")
        assertTrue(firstByte.get(3), "fixed header publish dup flag")
        assertFalse(firstByte.get(2), "fixed header qos bit 2")
        assertFalse(firstByte.get(1), "fixed header qos bit 1")
        assertFalse(firstByte.get(0), "fixed header retain flag")
        assertEquals(buffer.readVariableByteInteger(), 14u, "fixed header remaining length")
        assertEquals(8u, buffer.readUnsignedShort(), "variable header topic name length")
        assertEquals("user/log", buffer.readUtf8(8u).toString(), "variable header topic name value")
        if (publishMessage.variable.packetIdentifier != null) {
            assertEquals(buffer.readUnsignedShort().toInt(), publishMessage.variable.packetIdentifier)
        }
        assertEquals("yolo", buffer.readUtf8(4u).toString(), "payload value")
        buffer.resetForRead()
        val byte1 = buffer.readUnsignedByte()
        val remainingLength = buffer.readVariableByteInteger()
        val result = PublishMessage.from<String>(buffer, byte1, remainingLength)
        assertEquals(publishMessage, result)
    }

    @Test
    fun genericSerializationPublishQos1() {
        val publishMessage = PublishMessage.buildTyped(
            topicName = "user/log", payload = "yolo", qos = QualityOfService.AT_LEAST_ONCE, packetIdentifier = 13
        )
        val buffer = allocateNewBuffer(18u)
        publishMessage.serialize(buffer)
        buffer.resetForRead()
        val firstByte = buffer.readUnsignedByte()
        assertEquals(firstByte.toInt().shr(4), 3, "fixed header control packet type")
        assertFalse(firstByte.get(3), "fixed header publish dup flag")
        assertFalse(firstByte.get(2), "fixed header qos bit 2")
        assertTrue(firstByte.get(1), "fixed header qos bit 1")
        assertFalse(firstByte.get(0), "fixed header retain flag")
        assertEquals(buffer.readVariableByteInteger(), 16u, "fixed header remaining length")
        assertEquals(8u, buffer.readUnsignedShort(), "variable header topic name length")
        assertEquals("user/log", buffer.readUtf8(8u).toString(), "variable header topic name value")
        if (publishMessage.variable.packetIdentifier != null) {
            assertEquals(buffer.readUnsignedShort().toInt(), publishMessage.variable.packetIdentifier)
        }
        assertEquals("yolo", buffer.readUtf8(4u).toString(), "payload value")
        buffer.resetForRead()
        val byte1 = buffer.readUnsignedByte()
        val remainingLength = buffer.readVariableByteInteger()
        val result = PublishMessage.from<String>(buffer, byte1, remainingLength)
        assertEquals(publishMessage, result)
    }

    @Test
    fun genericSerializationPublishQos2() {
        val publishMessage = PublishMessage.buildTyped(
            topicName = "user/log", payload = "yolo", qos = QualityOfService.EXACTLY_ONCE, packetIdentifier = 13
        )
        val buffer = allocateNewBuffer(18u)
        publishMessage.serialize(buffer)
        buffer.resetForRead()
        val firstByte = buffer.readUnsignedByte()
        assertEquals(firstByte.toInt().shr(4), 3, "fixed header control packet type")
        assertFalse(firstByte.get(3), "fixed header publish dup flag")
        assertTrue(firstByte.get(2), "fixed header qos bit 2")
        assertFalse(firstByte.get(1), "fixed header qos bit 1")
        assertFalse(firstByte.get(0), "fixed header retain flag")
        assertEquals(buffer.readVariableByteInteger(), 16u, "fixed header remaining length")
        assertEquals(8u, buffer.readUnsignedShort(), "variable header topic name length")
        assertEquals("user/log", buffer.readUtf8(8u).toString(), "variable header topic name value")
        if (publishMessage.variable.packetIdentifier != null) {
            assertEquals(buffer.readUnsignedShort().toInt(), publishMessage.variable.packetIdentifier)
        }
        assertEquals("yolo", buffer.readUtf8(4u).toString(), "payload value")
        buffer.resetForRead()
        val byte1 = buffer.readUnsignedByte()
        val remainingLength = buffer.readVariableByteInteger()
        val result = PublishMessage.from<String>(buffer, byte1, remainingLength)
        assertEquals(publishMessage, result)
    }

    @Test
    fun genericSerializationPublishRetainFlag() {
        val publishMessage = PublishMessage.buildTyped(topicName = "user/log", payload = "yolo", retain = true)
        val buffer = allocateNewBuffer(16u)
        publishMessage.serialize(buffer)
        buffer.resetForRead()
        val firstByte = buffer.readUnsignedByte()
        assertEquals(firstByte.toInt().shr(4), 3, "fixed header control packet type")
        assertFalse(firstByte.get(3), "fixed header publish dup flag")
        assertFalse(firstByte.get(2), "fixed header qos bit 2")
        assertFalse(firstByte.get(1), "fixed header qos bit 1")
        assertTrue(firstByte.get(0), "fixed header retain flag")
        assertEquals(buffer.readVariableByteInteger(), 14u, "fixed header remaining length")
        assertEquals(8u, buffer.readUnsignedShort(), "variable header topic name length")
        assertEquals("user/log", buffer.readUtf8(8u).toString(), "variable header topic name value")
        if (publishMessage.variable.packetIdentifier != null) {
            assertEquals(buffer.readUnsignedShort().toInt(), publishMessage.variable.packetIdentifier)
        }
        assertEquals("yolo", buffer.readUtf8(4u).toString(), "payload value")
        buffer.resetForRead()
        val byte1 = buffer.readUnsignedByte()
        val remainingLength = buffer.readVariableByteInteger()
        val result = PublishMessage.from<String>(buffer, byte1, remainingLength)
        assertEquals(publishMessage, result)
    }

    @Test
    fun nullGenericSerialization() {
        val publishMessage = PublishMessage.build(topicName = "user/log")
        val buffer = allocateNewBuffer(12u)
        publishMessage.serialize(buffer)
        buffer.resetForRead()
        val firstByte = buffer.readUnsignedByte()
        assertEquals(firstByte.toInt().shr(4), 3, "fixed header control packet type")
        assertFalse(firstByte.get(3), "fixed header publish dup flag")
        assertFalse(firstByte.get(2), "fixed header qos bit 2")
        assertFalse(firstByte.get(1), "fixed header qos bit 1")
        assertFalse(firstByte.get(0), "fixed header retain flag")
        assertEquals(buffer.readVariableByteInteger(), 10u, "fixed header remaining length")
        assertEquals(8u, buffer.readUnsignedShort(), "variable header topic name length")
        assertEquals("user/log", buffer.readUtf8(8u).toString(), "variable header topic name value")
        if (publishMessage.variable.packetIdentifier != null) {
            assertEquals(buffer.readUnsignedShort().toInt(), publishMessage.variable.packetIdentifier)
        }
        buffer.resetForRead()
        val byte1 = buffer.readUnsignedByte()
        val remainingLength = buffer.readVariableByteInteger()
        val result = PublishMessage.from<Unit>(buffer, byte1, remainingLength)
        assertEquals(publishMessage, result)
    }
}
