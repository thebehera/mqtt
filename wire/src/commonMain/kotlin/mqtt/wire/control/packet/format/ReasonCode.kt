@file:Suppress("EXPERIMENTAL_API_USAGE")

package mqtt.wire.control.packet.format

import mqtt.wire.control.packet.format.fixed.ControlPacketType
import mqtt.wire.control.packet.format.fixed.ControlPacketType.*

/**
 * A Reason Code is a one byte unsigned value that indicates the result of an operation. Reason Codes less than 0x80
 * indicate successful completion of an operation. The normal Reason Code for success is 0. Reason Code values of 0x80
 * or greater indicate failure.
 * The CONNACK, PUBACK, PUBREC, PUBREL, PUBCOMP, DISCONNECT and AUTH Control Packets have a single Reason Code as part
 * of the Variable Header. The SUBACK and UNSUBACK packets contain a list of one or more Reason Codes in the Payload.
 */
enum class ReasonCode(val byte: UByte, vararg val packets: ControlPacketType) {
    SUCCESS(0x00.toUByte(), CONNACK, PUBACK, PUBREC, PUBREL, PUBCOMP, UNSUBACK, AUTH),
    NORMAL_DISCONNECTION(0x00.toUByte(), DISCONNECT),
    GRANTED_QOS_0(0x00.toUByte(), SUBACK),
    GRANTED_QOS_1(0x01.toUByte(), SUBACK),
    GRANTED_QOS_2(0x02.toUByte(), SUBACK),
    DISCONNECT_WITH_WILL_MESSAGE(0x04.toUByte(), DISCONNECT),
    NO_MATCHING_SUBSCRIBERS(0x10.toUByte(), PUBACK, PUBREL),
    NO_SUBSCRIPTIONS_EXISTED(0x11.toUByte(), UNSUBACK),
    CONTINUE_AUTHENTICATION(0x18.toUByte(), AUTH),
    REAUTHENTICATE(0x19.toUByte(), AUTH),
    UNSPECIFIED_ERROR(0x80.toUByte(), CONNACK, PUBACK, PUBREC, SUBACK, UNSUBACK, DISCONNECT),
    MALFORMED_PACKET(0x81.toUByte(), CONNACK, DISCONNECT),
    PROTOCOL_ERROR(0x82.toUByte(), CONNACK, DISCONNECT),
    IMPLEMENTATION_SPECIFIC_ERROR(0x83.toUByte(), CONNACK, PUBACK, PUBREC, SUBACK, UNSUBACK, DISCONNECT),
    UNSUPPORTED_PROTOCOL_VERSION(0x84.toUByte(), CONNACK),
    CLIENT_IDENTIFIER_NOT_VALID(0x85.toUByte(), CONNACK),
    BAD_USER_NAME_OR_PASSWORD(0x86.toUByte(), CONNACK),
    NOT_AUTHORIZED(0x87.toUByte(), CONNACK, PUBACK, PUBREC, SUBACK, UNSUBACK, DISCONNECT),
    SERVER_UNAVAILABLE(0x88.toUByte(), CONNACK),
    SERVER_BUSY(0x89.toUByte(), CONNECT, DISCONNECT),
    BANNED(0x8A.toUByte(), CONNACK),
    SERVER_SHUTTING_DOWN(0x8B.toUByte(), DISCONNECT),
    BAD_AUTHENTICATION_METHOD(0x8C.toUByte(), CONNACK, DISCONNECT),
    KEEP_ALIVE_TIMEOUT(0x8D.toUByte(), DISCONNECT),
    SESSION_TAKE_OVER(0x8E.toUByte(), DISCONNECT),
    TOPIC_FILTER_INVALID(0x8F.toUByte(), SUBACK, UNSUBACK, DISCONNECT),
    TOPIC_NAME_INVALID(0x90.toUByte(), CONNACK, PUBACK, PUBREC, DISCONNECT),
    /**
     * the response to this is either to try to fix the state, or to reset the Session state by connecting using Clean
     * Start set to 1, or to decide if the Client or Server implementations are defective.
     */
    PACKET_IDENTIFIER_IN_USE(0x91.toUByte(), PUBACK, PUBREC, SUBACK, UNSUBACK),
    PACKET_IDENTIFIER_NOT_FOUND(0x92.toUByte(), PUBREL, PUBCOMP),
    RECEIVE_MAXIMUM_EXCEEDED(0x93.toUByte(), DISCONNECT),
    TOPIC_ALIAS_INVALID(0x94.toUByte(), DISCONNECT),
    PACKET_TOO_LARGE(0x95.toUByte(), CONNACK, DISCONNECT),
    MESSAGE_RATE_TOO_HIGH(0x96.toUByte(), DISCONNECT),
    QUOTA_EXCEEDED(0x97.toUByte(), CONNACK, PUBACK, PUBREC, SUBACK, DISCONNECT),
    ADMINISTRATIVE_ACTION(0x98.toUByte(), DISCONNECT),
    PAYLOAD_FORMAT_INVALID(0x99.toUByte(), CONNACK, PUBACK, PUBREC, DISCONNECT),
    RETAIN_NOT_SUPPORTED(0x9A.toUByte(), CONNACK, DISCONNECT),
    QOS_NOT_SUPPORTED(0x9B.toUByte(), CONNACK, DISCONNECT),
    USE_ANOTHER_SERVER(0x9C.toUByte(), CONNACK, DISCONNECT),
    SERVER_MOVED(0x9D.toUByte(), CONNACK, DISCONNECT),
    SHARED_SUBSCRIPTIONS_NOT_SUPPORTED(0x9E.toUByte(), SUBACK, DISCONNECT),
    CONNECTION_RATE_EXCEEDED(0x9F.toUByte(), CONNACK, DISCONNECT),
    MAXIMUM_CONNECTION_TIME(0xA0.toUByte(), DISCONNECT),
    SUBSCRIPTION_IDENTIFIERS_NOT_SUPPORTED(0xA1.toUByte(), SUBACK, DISCONNECT),
    WILDCARD_SUBSCRIPTIONS_NOT_SUPPORTED(0xA2.toUByte(), SUBACK, DISCONNECT)
}