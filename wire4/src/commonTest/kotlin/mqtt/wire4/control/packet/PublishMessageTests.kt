@file:Suppress("EXPERIMENTAL_API_USAGE")

package mqtt.wire4.control.packet

import kotlinx.io.core.buildPacket
import mqtt.wire.MalformedPacketException
import mqtt.wire.data.MqttUtf8String
import mqtt.wire.data.QualityOfService
import mqtt.wire4.control.packet.PublishMessage.FixedHeader
import mqtt.wire4.control.packet.PublishMessage.VariableHeader
import kotlin.test.Test
import kotlin.test.fail

class PublishMessageTests {

    @Test
    fun qosBothBitsSetTo1ThrowsMalformedPacketException() {
        val byte1 = 0b00111110.toByte()
        val remainingLength = 1.toByte()
        val packet = buildPacket {
            writeByte(byte1)
            writeByte(remainingLength)
        }
        try {
            ControlPacket.from(packet)
            fail()
        } catch (e: MalformedPacketException) {
        }
    }

    @Test
    fun qos0AndPacketIdentifierThrowsIllegalArgumentException() {
        val fixed = FixedHeader(qos = QualityOfService.AT_MOST_ONCE)
        val variable = VariableHeader(MqttUtf8String("t"), 2.toUShort())
        try {
            PublishMessage(fixed, variable)
            fail()
        } catch (e: IllegalArgumentException) {
        }
    }

    @Test
    fun qos1WithoutPacketIdentifierThrowsIllegalArgumentException() {
        val fixed = FixedHeader(qos = QualityOfService.AT_LEAST_ONCE)
        val variable = VariableHeader(MqttUtf8String("t"))
        try {
            PublishMessage(fixed, variable)
            fail()
        } catch (e: IllegalArgumentException) {
        }
    }

    @Test
    fun qos2WithoutPacketIdentifierThrowsIllegalArgumentException() {
        val fixed = FixedHeader(qos = QualityOfService.EXACTLY_ONCE)
        val variable = VariableHeader(MqttUtf8String("t"))
        try {
            PublishMessage(fixed, variable)
            fail()
        } catch (e: IllegalArgumentException) {
        }
    }
}
