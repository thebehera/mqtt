package mqtt.wire.control.packet.format.variable.property

import kotlinx.io.core.BytePacketBuilder
import mqtt.wire.data.Type

data class RequestProblemInformation(val reasonStringOrUserPropertiesAreSentInFailures: Boolean)
    : Property(0x17, Type.BYTE) {
    override fun write(bytePacketBuilder: BytePacketBuilder) = write(bytePacketBuilder, reasonStringOrUserPropertiesAreSentInFailures)
}
