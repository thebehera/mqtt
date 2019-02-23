package mqtt.wire.control.packet.fixed

import mqtt.wire.control.packet.fixed.DirectionOfFlow.*
import mqtt.wire.data.QualityOfService

/**
 * The MQTT specification defines fifteen different types of MQTT Control Packet, for example the PUBLISH packet is
 * used to convey Application Messages.
 * @see https://docs.oasis-open.org/mqtt/mqtt/v5.0/cos02/mqtt-v5.0-cos02.html#_Toc1477322
 * @param value Value defined under [MQTT 2.1.2]
 * @param direction Direction of Flow defined under [MQTT 2.1.2]
 */
enum class ControlPacketType(val value: Byte, val direction: DirectionOfFlow) {
    RESERVED(0, FORBIDDEN),
    /**
     * Connection request
     */
    CONNECT(1, CLIENT_TO_SERVER),
    /**
     * Connect acknowledgment
     */
    CONNACK(2, SERVER_TO_CLIENT),
    /**
     * Publish message
     */
    PUBLISH(3, BIDIRECTIONAL),
    /**
     * Publish acknowledgment (QoS 1)
     */
    PUBACK(4, BIDIRECTIONAL),
    /**
     * Publish received (QoS 2 delivery part 1)
     */
    PUBREC(5, BIDIRECTIONAL),
    /**
     * Publish release (QoS 2 delivery part 2)
     */
    PUBREL(6, BIDIRECTIONAL),
    /**
     * Publish complete (QoS 2 delivery part 3)
     */
    PUBCOMP(7, BIDIRECTIONAL),
    /**
     * Subscribe request
     */
    SUBSCRIBE(8, CLIENT_TO_SERVER),
    /**
     * Subscribe acknowledgment
     */
    SUBACK(9, SERVER_TO_CLIENT),
    /**
     * Unsubscribe request
     */
    UNSUBSCRIBE(10, CLIENT_TO_SERVER),
    /**
     * Unsubscribe acknowledgment
     */
    UNSUBACK(11, SERVER_TO_CLIENT),
    /**
     * PING request
     */
    PINGREQ(12, CLIENT_TO_SERVER),
    /**
     * PING response
     */
    PINGRESP(13, SERVER_TO_CLIENT),
    /**
     * Disconnect notification
     */
    DISCONNECT(14, BIDIRECTIONAL),
    /**
     * Authentication exchange
     */
    AUTH(15, BIDIRECTIONAL);


    /**
     * Get the flags for the message itself. Parameters are ignored for any non PUBLISH message
     * @param dup Duplicate delivery of a PUBLISH packet
     * @param qos PUBLISH Quality of Service
     * @param retain PUBLISH retained message flag
     */
    fun flags(dup: Boolean = false, qos: QualityOfService = QualityOfService.AT_MOST_ONCE, retain: Boolean = false) =
            when (this) {
                RESERVED -> throw IllegalStateException("Not allowed to get flags for RESERVED")
                PUBLISH -> {
                    val qosBitInformation = qos.toBitInformation()
                    FlagBits(dup, qosBitInformation.first, qosBitInformation.second, retain)
                }
                PUBREL -> bit1TrueFlagBits
                SUBSCRIBE -> bit1TrueFlagBits
                UNSUBSCRIBE -> bit1TrueFlagBits
                else -> emptyFlagBits
            }
}
