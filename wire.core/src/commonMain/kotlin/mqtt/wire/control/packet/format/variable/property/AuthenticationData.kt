package mqtt.wire.control.packet.format.variable.property

import kotlinx.io.core.BytePacketBuilder
import mqtt.wire.data.ByteArrayWrapper
import mqtt.wire.data.Type

data class AuthenticationData(val data: ByteArrayWrapper) : Property(0x16, Type.BINARY_DATA) {
    override fun write(bytePacketBuilder: BytePacketBuilder) = write(bytePacketBuilder, data)
}
