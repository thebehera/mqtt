@file:Suppress("EXPERIMENTAL_API_USAGE")

package mqtt.wire5.control.packet.format.variable.property

import mqtt.buffer.GenericType
import mqtt.buffer.WriteBuffer
import mqtt.wire.data.Type

data class CorrelationData<T : Any>(val genericType: GenericType<T>) :
    Property(0x09, Type.BINARY_DATA, willProperties = true) {
    override fun size(buffer: WriteBuffer) = buffer.sizeGenericType(genericType.obj, genericType.kClass)
    override fun write(buffer: WriteBuffer): UInt {
        buffer.writeGenericType(genericType)
        return size(buffer)
    }
}
