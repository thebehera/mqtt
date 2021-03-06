@file:Suppress("EXPERIMENTAL_API_USAGE")

package mqtt.wire5.control.packet.format.variable.property

import mqtt.buffer.WriteBuffer
import mqtt.wire.data.Type

data class MaximumPacketSize(val packetSizeLimitationBytes: Long) : Property(0x27, Type.FOUR_BYTE_INTEGER) {
    override fun size() = size(packetSizeLimitationBytes.toUInt())
    override fun write(buffer: WriteBuffer) =
        write(buffer, packetSizeLimitationBytes.toUInt())
}
