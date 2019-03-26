package mqtt.wire5.control.packet.format.variable.property

import kotlinx.io.core.BytePacketBuilder
import mqtt.wire.data.ByteArrayWrapper
import mqtt.wire.data.Type

data class CorrelationData(val data: ByteArrayWrapper) : Property(0x09, Type.BINARY_DATA, willProperties = true) {
    override fun write(bytePacketBuilder: BytePacketBuilder) = write(bytePacketBuilder, data)
}
