@file:Suppress("EXPERIMENTAL_API_USAGE", "EXPERIMENTAL_UNSIGNED_LITERALS")

package mqtt.wire.control.packet

import mqtt.buffer.WriteBuffer
import mqtt.wire.MqttWarning
import mqtt.wire.control.packet.format.fixed.DirectionOfFlow

interface ControlPacket {
    val controlPacketValue: Byte
    val direction: DirectionOfFlow
    val flags: Byte get() = 0b0
    val mqttVersion: Byte

    val controlPacketReader: ControlPacketReader

    private fun fixedHeader(writeBuffer: WriteBuffer) {
        val packetValueUInt = controlPacketValue.toUInt()
        val packetValueShifted = packetValueUInt.shl(4)
        val localFlagsByte = flags.toUByte().toInt()
        val byte1 = (packetValueShifted.toByte() + localFlagsByte).toUByte()
        writeBuffer.write(byte1)
        val remaining = remainingLength(writeBuffer)
        writeBuffer.writeVariableByteInteger(remaining)
    }

    fun variableHeader(writeBuffer: WriteBuffer) {}
    fun payload(writeBuffer: WriteBuffer) {}

    fun remainingLength(buffer: WriteBuffer) = 0u

    fun validateOrGetWarning(): MqttWarning? {
        return null
    }

    fun serialize(writeBuffer: WriteBuffer) {
        fixedHeader(writeBuffer)
        variableHeader(writeBuffer)
        payload(writeBuffer)
    }

    companion object {
        fun isValidFirstByte(uByte: UByte): Boolean {
            val byte1AsUInt = uByte.toUInt()
            return byte1AsUInt.shr(4).toInt() in 1..15
        }
    }
}
