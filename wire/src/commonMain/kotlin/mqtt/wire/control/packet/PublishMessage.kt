@file:Suppress("EXPERIMENTAL_API_USAGE")

package mqtt.wire.control.packet

import mqtt.wire.control.packet.format.fixed.DirectionOfFlow
import mqtt.wire.data.QualityOfService

/**
 * Creates an MQTT PUBLISH
 * @param dup Duplicate delivery of a PublishMessage packet
 * @param qos PublishMessage Quality of Service
 * @param retain PublishMessage retained message flag
 * @param packetIdentifier Packet Identifier for QOS > 0 packet identifier
 */
data class PublishMessage(val dup: Boolean = false,
                          val qos: QualityOfService = QualityOfService.AT_MOST_ONCE,
                          val retain: Boolean = false,
                          val packetIdentifier: UShort? = null)
    : ControlPacket(3, DirectionOfFlow.BIDIRECTIONAL, flags(dup, qos, retain)) {
    init {
        if (qos == QualityOfService.AT_MOST_ONCE && packetIdentifier != null) {
            throw IllegalArgumentException("Cannot allocate a publish message with a QoS of 0 with a packet identifier")
        } else if (qos.isGreaterThan(QualityOfService.AT_MOST_ONCE) && packetIdentifier == null) {
            throw IllegalArgumentException("Cannot allocate a publish message with a QoS >0 and no packet identifier")
        }
    }

    companion object {
        fun flags(dup: Boolean, qos: QualityOfService, retain: Boolean): Byte {
            val dupInt = if (dup) 0b1000 else 0b0
            val qosInt = qos.integerValue.toInt().shl(1)
            val retainInt = if (retain) 0b1 else 0b0
            return (dupInt or qosInt or retainInt).toByte()
        }
    }
}
