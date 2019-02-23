package mqtt.wire.control.packet.variable

import mqtt.wire.control.packet.fixed.ControlPacketType
import mqtt.wire.control.packet.fixed.ControlPacketType.*
import mqtt.wire.data.QualityOfService

/**
 * Some types of MQTT Control Packet contain a Variable Header component. It resides between the Fixed Header and the
 * Payload. The content of the Variable Header varies depending on the packet type. The Packet Identifier field of
 * Variable Header is common in several packet types.
 * @see https://docs.oasis-open.org/mqtt/mqtt/v5.0/cos02/mqtt-v5.0-cos02.html#_Toc1477325
 */
interface VariableHeader {


    companion object {
        fun requiresPacketIdentifier(controlPacketType: ControlPacketType,
                                     qualityOfService: QualityOfService = QualityOfService.AT_MOST_ONCE) =
                when (controlPacketType) {
                    CONNECT -> false
                    CONNACK -> false
                    PINGREQ -> false
                    PINGRESP -> false
                    DISCONNECT -> false
                    AUTH -> false
                    PUBLISH -> qualityOfService.isGreaterThan(QualityOfService.AT_MOST_ONCE)
                    else -> true
                }
    }
}
