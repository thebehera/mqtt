package mqtt.wire5.control.packet.format.variable.property

import kotlinx.io.core.BytePacketBuilder
import mqtt.wire.data.Type

data class SubscriptionIdentifierAvailable(val serverSupported: Boolean) : Property(0x29, Type.BYTE) {
    override fun write(bytePacketBuilder: BytePacketBuilder) = write(bytePacketBuilder, serverSupported)
}
