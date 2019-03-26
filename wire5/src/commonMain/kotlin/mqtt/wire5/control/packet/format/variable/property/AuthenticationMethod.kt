package mqtt.wire5.control.packet.format.variable.property

import kotlinx.io.core.BytePacketBuilder
import mqtt.wire.data.MqttUtf8String
import mqtt.wire.data.Type

data class AuthenticationMethod(val value: MqttUtf8String) : Property(0x15, Type.UTF_8_ENCODED_STRING) {
    override fun write(bytePacketBuilder: BytePacketBuilder) = write(bytePacketBuilder, value)
}
