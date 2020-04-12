package mqtt.wire5.control.packet.format.variable.property

import mqtt.buffer.WriteBuffer
import mqtt.wire.data.MqttUtf8String
import mqtt.wire.data.Type

@ExperimentalUnsignedTypes
data class UserProperty(val key: MqttUtf8String, val value: MqttUtf8String) : Property(
    0x26,
    Type.UTF_8_STRING_PAIR, willProperties = true
) {
    override fun write(buffer: WriteBuffer): UInt {
        buffer.write(identifierByte)
        buffer.writeUtf8String(key.value)
        buffer.writeUtf8String(value.value)
        return size(buffer)
    }
    override fun size(buffer: WriteBuffer) = 5u + buffer.mqttUtf8Size(key.value) + buffer.mqttUtf8Size(value.value)
}
