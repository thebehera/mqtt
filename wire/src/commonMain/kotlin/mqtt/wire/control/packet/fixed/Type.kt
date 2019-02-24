@file:Suppress("EXPERIMENTAL_API_USAGE")

package mqtt.wire.control.packet.fixed

import mqtt.wire.MalformedPacketException
import mqtt.wire.control.packet.Payload
import mqtt.wire.control.packet.Payload.*
import mqtt.wire.control.packet.fixed.ControlPacketType.*
import mqtt.wire.control.packet.fixed.DirectionOfFlow.*
import mqtt.wire.data.QualityOfService
import mqtt.wire.data.QualityOfService.AT_MOST_ONCE

/**
 * The MQTT specification defines fifteen different types of MQTT Control Packet, for example the PUBLISH packet is
 * used to convey Application Messages.
 * @see https://docs.oasis-open.org/mqtt/mqtt/v5.0/cos02/mqtt-v5.0-cos02.html#_Toc1477322
 * @see http://docs.oasis-open.org/mqtt/mqtt/v5.0/cos01/mqtt-v5.0-cos01.html#_Toc514847903
 * @param value Value defined under [MQTT 2.1.2]
 * @param direction Direction of Flow defined under [MQTT 2.1.2]
 * @param payloadRequired Is the payload required? see the second url
 */
enum class ControlPacketType(val value: Byte, val direction: DirectionOfFlow, val payloadRequired: Payload) {
    RESERVED(0, FORBIDDEN, NONE),
    /**
     * Connection request
     */
    CONNECT(1, CLIENT_TO_SERVER, REQUIRED),
    /**
     * Connect acknowledgment
     */
    CONNACK(2, SERVER_TO_CLIENT, NONE),
    /**
     * Publish message
     */
    PUBLISH(3, BIDIRECTIONAL, OPTIONAL),
    /**
     * Publish acknowledgment (QoS 1)
     */
    PUBACK(4, BIDIRECTIONAL, NONE),
    /**
     * Publish received (QoS 2 delivery part 1)
     */
    PUBREC(5, BIDIRECTIONAL, NONE),
    /**
     * Publish release (QoS 2 delivery part 2)
     */
    PUBREL(6, BIDIRECTIONAL, NONE),
    /**
     * Publish complete (QoS 2 delivery part 3)
     */
    PUBCOMP(7, BIDIRECTIONAL, NONE),
    /**
     * Subscribe request
     */
    SUBSCRIBE(8, CLIENT_TO_SERVER, REQUIRED),
    /**
     * Subscribe acknowledgment
     */
    SUBACK(9, SERVER_TO_CLIENT, REQUIRED),
    /**
     * Unsubscribe request
     */
    UNSUBSCRIBE(10, CLIENT_TO_SERVER, REQUIRED),
    /**
     * Unsubscribe acknowledgment
     */
    UNSUBACK(11, SERVER_TO_CLIENT, REQUIRED),
    /**
     * PING request
     */
    PINGREQ(12, CLIENT_TO_SERVER, NONE),
    /**
     * PING response
     */
    PINGRESP(13, SERVER_TO_CLIENT, NONE),
    /**
     * Disconnect notification
     */
    DISCONNECT(14, BIDIRECTIONAL, NONE),
    /**
     * Authentication exchange
     */
    AUTH(15, BIDIRECTIONAL, NONE);


    /**
     * Get the flags for the message itself. Parameters are ignored for any non PUBLISH message
     * @param dup Duplicate delivery of a PUBLISH packet
     * @param qos PUBLISH Quality of Service
     * @param retain PUBLISH retained message flag
     */
    fun flags(dup: Boolean = false, qos: QualityOfService = AT_MOST_ONCE, retain: Boolean = false) =
            when (this) {
                PUBLISH -> {
                    val qosBitInformation = qos.toBitInformation()
                    FlagBits(dup, qosBitInformation.first, qosBitInformation.second, retain)
                }
                PUBREL -> bit1TrueFlagBits
                SUBSCRIBE -> bit1TrueFlagBits
                UNSUBSCRIBE -> bit1TrueFlagBits
                else -> emptyFlagBits
            }


    /**
     * @see http://docs.oasis-open.org/mqtt/mqtt/v5.0/cos01/mqtt-v5.0-cos01.html#_Toc514847893
     */
    fun requiresPacketIdentifier(qualityOfService: QualityOfService = AT_MOST_ONCE) =
            when (this) {
                CONNECT -> false
                CONNACK -> false
                PINGREQ -> false
                PINGRESP -> false
                DISCONNECT -> false
                AUTH -> false
                PUBLISH -> qualityOfService.isGreaterThan(AT_MOST_ONCE)
                else -> true
            }

    /**
     * @see http://docs.oasis-open.org/mqtt/mqtt/v5.0/cos01/mqtt-v5.0-cos01.html#_Toc514847900
     */
    fun requiresProperties(controlPacketType: ControlPacketType) =
            when (controlPacketType) {
                CONNECT -> true
                CONNACK -> true
                PUBLISH -> true
                PUBACK -> true
                PUBREC -> true
                PUBREL -> true
                PUBCOMP -> true
                SUBSCRIBE -> true
                SUBACK -> true
                UNSUBACK -> true
                DISCONNECT -> true
                AUTH -> true
                else -> false
            }
}

internal fun Byte.toControlPacketType(): ControlPacketType {
    val uByte = this.toUByte()
    val int = uByte.toInt()
    val shiftedRight = int.shr(4)
    val byte = shiftedRight.toByte()
    return when (byte) {
        RESERVED.value -> RESERVED
        CONNECT.value -> CONNECT
        CONNACK.value -> CONNACK
        PUBLISH.value -> PUBLISH
        PUBACK.value -> PUBACK
        PUBREC.value -> PUBREC
        PUBREL.value -> PUBREL
        PUBCOMP.value -> PUBCOMP
        SUBSCRIBE.value -> SUBSCRIBE
        SUBACK.value -> SUBACK
        UNSUBSCRIBE.value -> UNSUBSCRIBE
        UNSUBACK.value -> UNSUBACK
        PINGREQ.value -> PINGREQ
        PINGRESP.value -> PINGRESP
        DISCONNECT.value -> DISCONNECT
        AUTH.value -> AUTH
        else -> throw MalformedPacketException("Invalid byte1 header")
    }
}