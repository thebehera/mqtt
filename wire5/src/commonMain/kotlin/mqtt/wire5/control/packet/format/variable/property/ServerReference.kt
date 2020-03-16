package mqtt.wire5.control.packet.format.variable.property

import kotlinx.io.core.BytePacketBuilder
import mqtt.buffer.WriteBuffer
import mqtt.wire.data.MqttUtf8String
import mqtt.wire.data.Type

data class ServerReference(val otherServer: MqttUtf8String) : Property(0x1C, Type.UTF_8_ENCODED_STRING) {
    override fun write(bytePacketBuilder: BytePacketBuilder) = write(bytePacketBuilder, otherServer)

    override fun write(buffer: WriteBuffer) = write(buffer, otherServer.value)
    override fun size(buffer: WriteBuffer) = size(buffer, otherServer.value)
}
