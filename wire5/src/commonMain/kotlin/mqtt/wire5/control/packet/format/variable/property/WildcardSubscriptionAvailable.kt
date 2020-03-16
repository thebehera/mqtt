package mqtt.wire5.control.packet.format.variable.property

import kotlinx.io.core.BytePacketBuilder
import mqtt.buffer.WriteBuffer
import mqtt.wire.data.Type

data class WildcardSubscriptionAvailable(val serverSupported: Boolean) : Property(0x28, Type.BYTE) {
    override fun write(bytePacketBuilder: BytePacketBuilder) = write(bytePacketBuilder, serverSupported)

    override fun size(buffer: WriteBuffer) = 2u
    override fun write(buffer: WriteBuffer) = write(buffer, serverSupported)
}
