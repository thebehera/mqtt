package mqtt.wire5.control.packet.format.variable.property

import kotlinx.io.core.BytePacketBuilder
import mqtt.buffer.WriteBuffer
import mqtt.wire.data.MqttUtf8String
import mqtt.wire.data.Type

data class ContentType(val value: MqttUtf8String) : Property(0x03, Type.UTF_8_ENCODED_STRING, willProperties = true) {
    override fun write(bytePacketBuilder: BytePacketBuilder) = write(bytePacketBuilder, value)
    override fun write(buffer: WriteBuffer) = write(buffer, value.value)
    override fun size(buffer: WriteBuffer) = size(buffer, value.value)
}
