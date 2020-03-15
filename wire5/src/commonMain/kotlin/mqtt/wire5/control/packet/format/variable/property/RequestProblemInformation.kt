package mqtt.wire5.control.packet.format.variable.property

import kotlinx.io.core.BytePacketBuilder
import mqtt.buffer.WriteBuffer
import mqtt.wire.data.Type

data class RequestProblemInformation(val reasonStringOrUserPropertiesAreSentInFailures: Boolean)
    : Property(0x17, Type.BYTE) {
    override fun write(bytePacketBuilder: BytePacketBuilder) =
        write(bytePacketBuilder, reasonStringOrUserPropertiesAreSentInFailures)

    override fun size(buffer: WriteBuffer) = 2u
    override fun write(buffer: WriteBuffer) = write(buffer, reasonStringOrUserPropertiesAreSentInFailures)
}
