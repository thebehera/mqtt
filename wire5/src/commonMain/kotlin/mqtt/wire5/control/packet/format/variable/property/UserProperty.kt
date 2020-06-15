package mqtt.wire5.control.packet.format.variable.property

import mqtt.buffer.WriteBuffer
import mqtt.wire.data.Type

@ExperimentalUnsignedTypes
data class UserProperty(val key: CharSequence, val value: CharSequence) : Property(
    0x26,
    Type.UTF_8_STRING_PAIR, willProperties = true
) {
    override fun write(buffer: WriteBuffer): UInt {
        buffer.write(identifierByte)
        buffer.writeMqttUtf8String(key)
        buffer.writeMqttUtf8String(value)
        return size(buffer)
    }

    override fun size(buffer: WriteBuffer) = 5u + buffer.lengthUtf8String(key) + buffer.lengthUtf8String(value)
}
