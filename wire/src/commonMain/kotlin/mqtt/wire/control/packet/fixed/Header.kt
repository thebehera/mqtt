package mqtt.wire.control.packet.fixed

import mqtt.wire.data.QualityOfService
import mqtt.wire.data.encodeVariableByteInteger

/**
 * Each MQTT Control Packet contains a Fixed Header
 * @see https://docs.oasis-open.org/mqtt/mqtt/v5.0/cos02/mqtt-v5.0-cos02.html#_Toc1477321
 */
interface FixedHeader {
    val controlPacketType: ControlPacketType
    val flags: FlagBits

    /**
     * The Remaining Length is a Variable Byte Integer that represents the number of bytes remaining within the current
     * Control Packet, including data in the Variable Header and the Payload. The Remaining Length does not include the
     * bytes used to encode the Remaining Length. The packet size is the total number of bytes in an MQTT Control
     * Packet, this is equal to the length of the Fixed Header plus the Remaining Length.
     */
    val remainingLength: Int
}

fun FixedHeader.remainingLengthVariableByteInteger() = remainingLength.encodeVariableByteInteger()

/**
 * Get the message ready in a . Parameters are ignored for any non PUBLISH message
 * @param dup Duplicate delivery of a PUBLISH packet
 * @param qos PUBLISH Quality of Service
 * @param retain PUBLISH retained message flag
 */
fun FixedHeader.toByteArray(dup: Boolean = false, qos: QualityOfService = QualityOfService.AT_MOST_ONCE,
                            retain: Boolean = false): ByteArray {
    val byte1 = (controlPacketType.value.toInt().shl(4) + controlPacketType.flags().toByte()).toByte()
    val byte2 = remainingLengthVariableByteInteger()
    return byteArrayOf(byte1, byte2[0])
}
