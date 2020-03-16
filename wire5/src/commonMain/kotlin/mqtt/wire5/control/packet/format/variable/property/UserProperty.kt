package mqtt.wire5.control.packet.format.variable.property

import kotlinx.io.core.BytePacketBuilder
import mqtt.buffer.WriteBuffer
import mqtt.wire.data.MqttUtf8String
import mqtt.wire.data.Type
import mqtt.wire.data.writeMqttUtf8String

@ExperimentalUnsignedTypes
data class UserProperty(val key: MqttUtf8String, val value: MqttUtf8String) : Property(
    0x26,
    Type.UTF_8_STRING_PAIR, willProperties = true
) {
    override fun write(bytePacketBuilder: BytePacketBuilder) {
        bytePacketBuilder.writeByte(identifierByte)
        bytePacketBuilder.writeMqttUtf8String(key)
        bytePacketBuilder.writeMqttUtf8String(value)
    }

    override fun write(buffer: WriteBuffer): UInt {
        println("write props $buffer")
        buffer.write(identifierByte)
        println("wrote $identifierByte $buffer")
        buffer.writeUtf8String(key.value)
        println("wrote ${key.value} $buffer")
        buffer.writeUtf8String(value.value)
        println("done  ${value.value}  $buffer")
        return size(buffer)
    }

    override fun size(buffer: WriteBuffer) = 5u + buffer.mqttUtf8Size(key.value) + buffer.mqttUtf8Size(value.value)
}
