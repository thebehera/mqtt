@file:Suppress("EXPERIMENTAL_API_USAGE")

package mqtt.wire.control.packet.variable

import mqtt.wire.control.packet.fixed.ControlPacketType
import mqtt.wire.data.QualityOfService
import mqtt.wire.data.QualityOfService.AT_MOST_ONCE

/**
 * Some types of MQTT Control Packet contain a Variable Header component. It resides between the Fixed Header and the
 * Payload. The content of the Variable Header varies depending on the packet type. The Packet Identifier field of
 * Variable Header is common in several packet types.
 * @see https://docs.oasis-open.org/mqtt/mqtt/v5.0/cos02/mqtt-v5.0-cos02.html#_Toc1477325
 */
data class VariableHeader constructor(val packetIdentifier: UShort?) {


    companion object {

        fun build(controlPacketType: ControlPacketType, qualityOfService: QualityOfService = AT_MOST_ONCE,
                  packetIdentifier: UShort? = null): VariableHeader? {
            if (!controlPacketType.requiresPacketIdentifier(qualityOfService)) {
                return null
            }
            return VariableHeader(packetIdentifier)
        }

    }
}
