@file:Suppress("EXPERIMENTAL_API_USAGE", "EXPERIMENTAL_UNSIGNED_LITERALS")

package mqtt.wire5.control.packet.format.variable.property

import mqtt.buffer.WriteBuffer
import mqtt.wire.MalformedPacketException
import mqtt.wire.data.QualityOfService
import mqtt.wire.data.Type

data class MaximumQos(val qos: QualityOfService) : Property(0x24, Type.BYTE) {
    override fun size() = 2u

    override fun write(buffer: WriteBuffer) = when (qos) {
        QualityOfService.AT_MOST_ONCE -> write(buffer, false)
        QualityOfService.AT_LEAST_ONCE -> write(buffer, true)
        QualityOfService.EXACTLY_ONCE -> throw MalformedPacketException("Max QoS Cannot be >= 2 as defined https://docs.oasis-open.org/mqtt/mqtt/v5.0/mqtt-v5.0.html#_Toc514847957")
    }
}
