package mqtt.wire.control.packet.format.variable.property

import kotlinx.io.core.BytePacketBuilder
import mqtt.wire.data.MqttUtf8String
import mqtt.wire.data.Type

data class ContentType(val value: MqttUtf8String) : Property(0x03, Type.UTF_8_ENCODED_STRING, willProperties = true) {
    override fun write(bytePacketBuilder: BytePacketBuilder) = write(bytePacketBuilder, value)
}
