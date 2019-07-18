package mqtt.wire.control.packet.format.variable.property

import kotlinx.io.core.BytePacketBuilder
import mqtt.wire.data.MqttUtf8String
import mqtt.wire.data.Type
import mqtt.wire.data.writeMqttUtf8String

data class UserProperty(val key: MqttUtf8String, val value: MqttUtf8String) : Property(
    0x26,
    Type.UTF_8_ENCODED_STRING, willProperties = true
) {
    override fun write(bytePacketBuilder: BytePacketBuilder) {
        bytePacketBuilder.writeByte(identifierByte)
        bytePacketBuilder.writeMqttUtf8String(key)
        bytePacketBuilder.writeMqttUtf8String(value)
    }
}
