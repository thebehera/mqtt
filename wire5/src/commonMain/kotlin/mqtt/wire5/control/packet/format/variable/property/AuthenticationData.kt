@file:Suppress("EXPERIMENTAL_API_USAGE", "EXPERIMENTAL_UNSIGNED_LITERALS")

package mqtt.wire5.control.packet.format.variable.property

import mqtt.buffer.WriteBuffer
import mqtt.wire.buffer.GenericSerialization
import mqtt.wire.buffer.GenericType
import mqtt.wire.buffer.writeGenericType
import mqtt.wire.data.Type

data class AuthenticationData<T : Any>(val data: GenericType<T>) : Property(0x16, Type.BINARY_DATA) {
    override fun size() =
        1u + UShort.SIZE_BYTES.toUInt() + GenericSerialization.size(data.obj, data.kClass)

    override fun write(buffer: WriteBuffer): UInt {
        buffer.write(identifierByte)
        buffer.write(GenericSerialization.size(data.obj, data.kClass).toUShort())
        buffer.writeGenericType(data)
        return size()
    }
}
