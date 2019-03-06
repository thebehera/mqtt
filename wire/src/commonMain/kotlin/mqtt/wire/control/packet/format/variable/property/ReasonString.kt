package mqtt.wire.control.packet.format.variable.property

import kotlinx.io.core.BytePacketBuilder
import mqtt.wire.data.MqttUtf8String
import mqtt.wire.data.Type

data class ReasonString(val diagnosticInfoDontParse: MqttUtf8String) : Property(0x1F, Type.UTF_8_ENCODED_STRING) {
    override fun write(bytePacketBuilder: BytePacketBuilder) = write(bytePacketBuilder, diagnosticInfoDontParse)
}
