@file:Suppress("EXPERIMENTAL_API_USAGE")

package mqtt.wire.control.packet.format.variable.property

import kotlinx.io.core.BytePacketBuilder
import mqtt.wire.data.Type

data class ReceiveMaximum(val maxQos1Or2ConcurrentMessages: UShort) : Property(0x21, Type.TWO_BYTE_INTEGER) {
    override fun write(bytePacketBuilder: BytePacketBuilder) = write(bytePacketBuilder, maxQos1Or2ConcurrentMessages)
}
