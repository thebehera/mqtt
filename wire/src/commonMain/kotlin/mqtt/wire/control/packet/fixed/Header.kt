package mqtt.wire.control.packet.fixed

import mqtt.wire.data.encodeVariableByteInteger

/**
 * Each MQTT Control Packet contains a Fixed Header
 * @see https://docs.oasis-open.org/mqtt/mqtt/v5.0/cos02/mqtt-v5.0-cos02.html#_Toc1477321
 */
interface FixedHeader {
    val controlPacketType: ControlPacketType

    /**
     * The Remaining Length is a Variable Byte Integer that represents the number of bytes remaining within the current
     * Control Packet, including data in the Variable Header and the Payload. The Remaining Length does not include the
     * bytes used to encode the Remaining Length. The packet size is the total number of bytes in an MQTT Control
     * Packet, this is equal to the length of the Fixed Header plus the Remaining Length.
     */
    val remainingLength: Int
}

fun FixedHeader.remainingLengthVariableByteInteger() = remainingLength.encodeVariableByteInteger()
