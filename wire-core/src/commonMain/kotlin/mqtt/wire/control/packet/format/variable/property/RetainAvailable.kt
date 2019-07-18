package mqtt.wire.control.packet.format.variable.property

import kotlinx.io.core.BytePacketBuilder
import mqtt.wire.data.Type

data class RetainAvailable(val serverSupported: Boolean) : Property(0x25, Type.BYTE) {
    override fun write(bytePacketBuilder: BytePacketBuilder) = write(bytePacketBuilder, serverSupported)
}
