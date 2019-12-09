@file:Suppress("EXPERIMENTAL_API_USAGE")

package mqtt.wire5.control.packet.format.variable.property

import kotlinx.io.core.BytePacketBuilder
import mqtt.wire.data.Type

data class WillDelayInterval(val seconds: Long) : Property(0x18, Type.FOUR_BYTE_INTEGER, willProperties = true) {
    override fun write(bytePacketBuilder: BytePacketBuilder) = write(bytePacketBuilder, seconds.toUInt())
}
