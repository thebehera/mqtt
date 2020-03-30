@file:Suppress("EXPERIMENTAL_API_USAGE")

package mqtt.wire.control.packet

import mqtt.buffer.ReadBuffer

interface ControlPacketReader {
    fun from(buffer: ReadBuffer): ControlPacket {
        val byte1 = buffer.readUnsignedByte()
        val remainingLength = buffer.readVariableByteInteger()
        return from(buffer, byte1, remainingLength)
    }

    fun from(buffer: ReadBuffer, byte1: UByte, remainingLength: UInt): ControlPacket
}