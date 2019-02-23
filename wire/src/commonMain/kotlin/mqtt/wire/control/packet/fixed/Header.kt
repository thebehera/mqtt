package mqtt.wire.control.packet.fixed

import mqtt.wire.control.packet.fixed.ControlPacketType.PUBLISH
import mqtt.wire.data.QualityOfService
import mqtt.wire.data.QualityOfService.AT_MOST_ONCE
import mqtt.wire.data.encodeVariableByteInteger

/**
 * Each MQTT Control Packet contains a Fixed Header
 * @see https://docs.oasis-open.org/mqtt/mqtt/v5.0/cos02/mqtt-v5.0-cos02.html#_Toc1477321
 */
data class FixedHeader(
        val controlPacketType: ControlPacketType,
        val flags: FlagBits
) {
    companion object {
        fun fromType(type: ControlPacketType, dup: Boolean = false,
                     qos: QualityOfService = AT_MOST_ONCE, retain: Boolean = false): FixedHeader {
            val flags = when (type) {
                PUBLISH -> type.flags(dup, qos, retain)
                else -> type.flags()
            }
            return FixedHeader(type, flags)
        }
    }
}

/**
 * The Remaining Length is a Variable Byte Integer that represents the number of bytes remaining within the current
 * Control Packet, including data in the Variable Header and the Payload. The Remaining Length does not include the
 * bytes used to encode the Remaining Length. The packet size is the total number of bytes in an MQTT Control
 * Packet, this is equal to the length of the Fixed Header plus the Remaining Length.
 */
internal fun remainingLengthVariableByteInteger(remainingLength: Int) = remainingLength.encodeVariableByteInteger()

fun FixedHeader.toByteArray(remainingLength: Int): ByteArray {
    val packetValue = controlPacketType.value
    val packetValueInt = packetValue.toInt()
    val packetValueShifted = packetValueInt.shl(4)
    val localFlags = flags
    val localFlagsByte = localFlags.toByte()

    val byte1 = (packetValueShifted.toByte() + localFlagsByte).toByte()
    val byte2 = remainingLengthVariableByteInteger(remainingLength)
    return byteArrayOf(byte1, *byte2)
}
