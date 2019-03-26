package mqtt.wire.control.packet.format.variable.property

import kotlinx.io.core.BytePacketBuilder
import mqtt.wire.data.Type

data class RequestResponseInformation(val requestServerToReturnInfoInConnack: Boolean)
    : Property(0x19, Type.BYTE) {
    override fun write(bytePacketBuilder: BytePacketBuilder) = write(bytePacketBuilder,
            requestServerToReturnInfoInConnack)
}
