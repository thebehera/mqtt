package mqtt.wire.control.packet.format.variable.property

import kotlinx.io.core.BytePacketBuilder
import mqtt.wire.MalformedPacketException
import mqtt.wire.data.QualityOfService
import mqtt.wire.data.Type

data class MaximumQos(val qos: QualityOfService) : Property(0x24, Type.BYTE) {
    override fun write(bytePacketBuilder: BytePacketBuilder) {
        when (qos) {
            QualityOfService.AT_MOST_ONCE -> write(bytePacketBuilder, false)
            QualityOfService.AT_LEAST_ONCE -> write(bytePacketBuilder, true)
            QualityOfService.EXACTLY_ONCE -> throw MalformedPacketException("Max QoS Cannot be >= 2 as defined http://docs.oasis-open.org/mqtt/mqtt/v5.0/cos01/mqtt-v5.0-cos01.html#_Toc514847957")
        }
    }
}
