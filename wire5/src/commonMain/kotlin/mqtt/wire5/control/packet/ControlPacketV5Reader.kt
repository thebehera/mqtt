@file:Suppress("EXPERIMENTAL_API_USAGE")

package mqtt.wire5.control.packet

import mqtt.buffer.ReadBuffer
import mqtt.wire.control.packet.ControlPacketReader

object ControlPacketV5Reader : ControlPacketReader {
    override fun from(buffer: ReadBuffer, byte1: UByte, remainingLength: UInt) =
        ControlPacketV5.from(buffer, byte1, remainingLength)


    override fun pingRequest() = PingRequest
    override fun pingResponse() = PingResponse
}