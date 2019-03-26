package mqtt.wire5.control.packet.format.variable.property

import kotlinx.io.core.BytePacketBuilder
import mqtt.wire.data.Type

data class PayloadFormatIndicator(val willMessageIsUtf8: Boolean) : Property(0x01, Type.BYTE,
        willProperties = true) {
    override fun write(bytePacketBuilder: BytePacketBuilder) = write(bytePacketBuilder, willMessageIsUtf8)
}
