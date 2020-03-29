@file:Suppress("EXPERIMENTAL_API_USAGE")

package mqtt.wire.control.packet

import mqtt.buffer.ReadBuffer

interface ControlPacketReader {
    fun from(buffer: ReadBuffer, byte1: UByte, remainingLength: UInt): ControlPacket
}