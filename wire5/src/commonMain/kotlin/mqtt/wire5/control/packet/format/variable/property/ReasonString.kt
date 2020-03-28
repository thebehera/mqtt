@file:Suppress("EXPERIMENTAL_API_USAGE")

package mqtt.wire5.control.packet.format.variable.property

import kotlinx.io.core.BytePacketBuilder
import mqtt.buffer.WriteBuffer
import mqtt.wire.data.MqttUtf8String
import mqtt.wire.data.Type

data class ReasonString(val diagnosticInfoDontParse: MqttUtf8String) : Property(0x1F, Type.UTF_8_ENCODED_STRING) {
    override fun write(bytePacketBuilder: BytePacketBuilder) = write(bytePacketBuilder, diagnosticInfoDontParse)
    override fun write(buffer: WriteBuffer) = write(buffer, diagnosticInfoDontParse.value)
    override fun size(buffer: WriteBuffer) = size(buffer, diagnosticInfoDontParse.value)
}
