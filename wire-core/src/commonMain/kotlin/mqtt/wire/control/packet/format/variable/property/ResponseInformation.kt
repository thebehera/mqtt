package mqtt.wire.control.packet.format.variable.property

import kotlinx.io.core.BytePacketBuilder
import mqtt.wire.data.MqttUtf8String
import mqtt.wire.data.Type

data class ResponseInformation(val requestResponseInformationInConnack: MqttUtf8String) :
    Property(0x1A, Type.UTF_8_ENCODED_STRING) {
    override fun write(bytePacketBuilder: BytePacketBuilder) =
        write(bytePacketBuilder, requestResponseInformationInConnack)
}
