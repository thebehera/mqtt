@file:Suppress("EXPERIMENTAL_API_USAGE")

package mqtt.wire5.control.packet.format.variable.property

import kotlinx.io.core.BytePacketBuilder
import mqtt.buffer.WriteBuffer
import mqtt.wire.data.Type

data class TopicAliasMaximum(val highestValueSupported: Int) : Property(0x23, Type.TWO_BYTE_INTEGER) {
    override fun write(bytePacketBuilder: BytePacketBuilder) =
        write(bytePacketBuilder, highestValueSupported.toUShort())

    override fun size(buffer: WriteBuffer) = 3u
    override fun write(buffer: WriteBuffer) = write(buffer, highestValueSupported.toUShort())
}
