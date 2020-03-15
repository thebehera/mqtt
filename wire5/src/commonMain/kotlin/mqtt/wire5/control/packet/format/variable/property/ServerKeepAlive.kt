@file:Suppress("EXPERIMENTAL_API_USAGE")

package mqtt.wire5.control.packet.format.variable.property

import kotlinx.io.core.BytePacketBuilder
import mqtt.buffer.WriteBuffer
import mqtt.wire.data.Type

data class ServerKeepAlive(val seconds: Int) : Property(0x13, Type.TWO_BYTE_INTEGER) {
    override fun write(bytePacketBuilder: BytePacketBuilder) = write(bytePacketBuilder, seconds.toUShort())
    override fun size(buffer: WriteBuffer) = 3u
    override fun write(buffer: WriteBuffer) = write(buffer, seconds.toUShort())
}
