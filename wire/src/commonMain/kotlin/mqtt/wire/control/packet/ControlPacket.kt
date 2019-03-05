@file:Suppress("EXPERIMENTAL_API_USAGE")

package mqtt.wire.control.packet

import kotlinx.io.core.*
import mqtt.wire.MalformedPacketException
import mqtt.wire.control.packet.format.fixed.DirectionOfFlow
import mqtt.wire.data.VariableByteInteger
import mqtt.wire.data.decodeVariableByteInteger

/**
 * The MQTT specification defines fifteen different types of MQTT Control Packet, for example the PublishMessage packet is
 * used to convey Application Messages.
 * @see https://docs.oasis-open.org/mqtt/mqtt/v5.0/cos02/mqtt-v5.0-cos02.html#_Toc1477322
 * @see http://docs.oasis-open.org/mqtt/mqtt/v5.0/cos01/mqtt-v5.0-cos01.html#_Toc514847903
 * @param controlPacketValue Value defined under [MQTT 2.1.2]
 * @param direction Direction of Flow defined under [MQTT 2.1.2]
 */
abstract class ControlPacket(val controlPacketValue: Byte,
                             val direction: DirectionOfFlow,
                             val flags: Byte = 0b0) {

    private val fixedHeader by lazy {
        val packetValue = controlPacketValue
        val packetValueUInt = packetValue.toUInt()
        val packetValueShifted = packetValueUInt.shl(4)
        val localFlagsByte = flags.toUInt()
        val byte1 = (packetValueShifted.toByte().toUInt() + localFlagsByte).toUByte()
        val byte2 = VariableByteInteger(remainingLength)
        buildPacket {
            writeUByte(byte1)
            writePacket(byte2.encodedValue())
        }.readBytes()
    }

    open val variableHeaderPacket: ByteArray? = null
    open val payloadPacket: ByteArray? = null
    private val remainingLength by lazy {
        val variableHeaderSize = variableHeaderPacket?.size ?: 0
        val payloadSize = payloadPacket?.size ?: 0
        (variableHeaderSize + payloadSize).toUInt()
    }

    val serialize by lazy {
        buildPacket {
            writeFully(fixedHeader)
            val variableHeaderPacket = variableHeaderPacket
            if (variableHeaderPacket != null) {
                writeFully(variableHeaderPacket)
            }
            val payloadPacket = payloadPacket
            if (payloadPacket != null) {
                writeFully(payloadPacket)
            }
        }.readBytes()
    }
    companion object {
        fun from(buffer: ByteReadPacket): ControlPacket {
            val byte1AsUInt = buffer.readUByte().toUInt()
            val packetValue = byte1AsUInt.shr(4).toInt()
            buffer.decodeVariableByteInteger() // remaining Length
            return when (packetValue) {
                0x00 -> Reserved
                0x01 -> ConnectionRequest.from(buffer)
                else -> throw MalformedPacketException("Invalid MQTT Control Packet Type: $packetValue Should be in range between 0 and 15 inclusive")
            }
        }
    }
}
