package mqtt.wire5.control.packet.format.variable.property

import kotlinx.io.core.BytePacketBuilder
import mqtt.buffer.WriteBuffer
import mqtt.wire.data.MqttUtf8String
import mqtt.wire.data.Type

data class ReasonString(val diagnosticInfoDontParse: MqttUtf8String) : Property(0x1F, Type.UTF_8_ENCODED_STRING) {
    override fun write(bytePacketBuilder: BytePacketBuilder) = write(bytePacketBuilder, diagnosticInfoDontParse)
    override fun write(bytePacketBuilder: WriteBuffer) = write(bytePacketBuilder, diagnosticInfoDontParse.value)
    override fun size(bytePacketBuilder: WriteBuffer) = size(bytePacketBuilder, diagnosticInfoDontParse.value)
}
