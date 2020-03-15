package mqtt.wire5.control.packet.format.variable.property

import kotlinx.io.core.BytePacketBuilder
import mqtt.buffer.WriteBuffer
import mqtt.wire.data.ByteArrayWrapper
import mqtt.wire.data.Type

data class AuthenticationData(val data: ByteArrayWrapper) : Property(0x16, Type.BINARY_DATA) {
    override fun write(bytePacketBuilder: BytePacketBuilder) = write(bytePacketBuilder, data)
    override fun size(buffer: WriteBuffer) = size(buffer, data)
    override fun write(buffer: WriteBuffer) = write(buffer, data)
}
