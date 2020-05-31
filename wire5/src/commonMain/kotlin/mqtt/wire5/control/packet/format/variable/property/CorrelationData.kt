@file:Suppress("EXPERIMENTAL_API_USAGE", "EXPERIMENTAL_UNSIGNED_LITERALS")

package mqtt.wire5.control.packet.format.variable.property

import mqtt.buffer.GenericType
import mqtt.buffer.WriteBuffer
import mqtt.wire.data.Type

data class CorrelationData<T : Any>(val genericType: GenericType<T>) :
    Property(0x09, Type.BINARY_DATA, willProperties = true) {
    override fun size(buffer: WriteBuffer) =
        1u + UShort.SIZE_BYTES.toUInt() + buffer.sizeGenericType(genericType.obj, genericType.kClass)

    override fun write(buffer: WriteBuffer): UInt {
        buffer.write(identifierByte)
        buffer.write(buffer.sizeGenericType(genericType.obj, genericType.kClass).toUShort())
        buffer.writeGenericType(genericType)
        return size(buffer)
    }
}
