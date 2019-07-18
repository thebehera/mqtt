@file:Suppress("EXPERIMENTAL_API_USAGE")

package mqtt.wire.control.packet.format.variable.property

import kotlinx.io.core.BytePacketBuilder
import mqtt.wire.data.Type
import mqtt.wire.data.VariableByteInteger

data class SubscriptionIdentifier(val value: UInt) : Property(0x0B, Type.VARIABLE_BYTE_INTEGER) {
    override fun write(bytePacketBuilder: BytePacketBuilder) =
        write(bytePacketBuilder, VariableByteInteger(value).encodedValue())
}
