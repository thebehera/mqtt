@file:Suppress("EXPERIMENTAL_API_USAGE")

package mqtt.wire5.control.packet.format.variable.property

import kotlinx.io.core.BytePacketBuilder
import mqtt.buffer.WriteBuffer
import mqtt.wire.data.Type

data class WillDelayInterval(val seconds: Long) : Property(0x18, Type.FOUR_BYTE_INTEGER, willProperties = true) {
    override fun write(bytePacketBuilder: BytePacketBuilder) = write(bytePacketBuilder, seconds.toUInt())

    override fun size(buffer: WriteBuffer) = size(buffer, seconds.toUInt())
    override fun write(buffer: WriteBuffer) = write(buffer, seconds.toUInt())
}
