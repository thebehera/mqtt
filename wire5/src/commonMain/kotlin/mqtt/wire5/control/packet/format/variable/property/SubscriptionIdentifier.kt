@file:Suppress("EXPERIMENTAL_API_USAGE")

package mqtt.wire5.control.packet.format.variable.property

import kotlinx.io.core.BytePacketBuilder
import mqtt.buffer.WriteBuffer
import mqtt.wire.data.Type
import mqtt.wire.data.VariableByteInteger

data class SubscriptionIdentifier(val value: Long) : Property(0x0B, Type.VARIABLE_BYTE_INTEGER) {
    override fun write(bytePacketBuilder: BytePacketBuilder) =
        write(bytePacketBuilder, VariableByteInteger(value.toUInt()).encodedValue())

    override fun size(buffer: WriteBuffer) = buffer.variableByteIntegerSize(value.toUInt()) + 1u
    override fun write(buffer: WriteBuffer): UInt {
        buffer.write(identifierByte)
        buffer.writeVariableByteInteger(value.toUInt())
        return size(buffer)
    }
}
