@file:Suppress("EXPERIMENTAL_API_USAGE", "EXPERIMENTAL_UNSIGNED_LITERALS")

package mqtt.wire4.control.packet

import mqtt.buffer.allocateNewBuffer
import mqtt.wire.MalformedPacketException
import mqtt.wire.data.QualityOfService
import mqtt.wire4.control.packet.PublishMessage.FixedHeader
import mqtt.wire4.control.packet.PublishMessage.VariableHeader
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.fail

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
        val byte1 = buffer.readUnsignedByte()
        val remainingLength = buffer.readVariableByteInteger()
        val result = PublishMessage.from<Unit>(buffer, byte1, remainingLength)
        assertEquals(publishMessage, result)
    }
}
