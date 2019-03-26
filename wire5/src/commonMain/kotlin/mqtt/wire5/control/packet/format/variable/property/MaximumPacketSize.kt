@file:Suppress("EXPERIMENTAL_API_USAGE")

package mqtt.wire5.control.packet.format.variable.property

import kotlinx.io.core.BytePacketBuilder
import mqtt.wire.data.Type

data class MaximumPacketSize(val packetSizeLimitationBytes: UInt) : Property(0x27, Type.FOUR_BYTE_INTEGER) {
    override fun write(bytePacketBuilder: BytePacketBuilder) = write(bytePacketBuilder, packetSizeLimitationBytes)
}
