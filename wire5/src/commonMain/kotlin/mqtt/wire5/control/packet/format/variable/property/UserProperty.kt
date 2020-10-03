package mqtt.wire5.control.packet.format.variable.property

import mqtt.buffer.WriteBuffer
import mqtt.wire.data.Type
import mqtt.wire.data.utf8Length

@ExperimentalUnsignedTypes
data class UserProperty(val key: CharSequence, val value: CharSequence) : Property(
    0x26,
    Type.UTF_8_STRING_PAIR, willProperties = true
) {
    override fun write(buffer: WriteBuffer): UInt {
        buffer.write(identifierByte)
        buffer.writeMqttUtf8String(key)
        buffer.writeMqttUtf8String(value)
        return size()
    }

    override fun size() = (5 + key.utf8Length() + value.utf8Length()).toUInt()
}
