@file:Suppress("EXPERIMENTAL_API_USAGE")

package mqtt.wire5.control.packet.format.fixed

import mqtt.wire5.control.packet.format.ReasonCode
import kotlin.test.Test
import kotlin.test.assertEquals

class ReasonCodeTests {
    @Test
    fun Success() = assertEquals(ReasonCode.SUCCESS.byte, 0x00.toUByte())

    @Test
    fun NormalDisconnection() = assertEquals(ReasonCode.NORMAL_DISCONNECTION.byte, 0x00.toUByte())

    @Test
    fun GrantedQos0() = assertEquals(ReasonCode.GRANTED_QOS_0.byte, 0x00.toUByte())

    @Test
    fun GrantedQos1() = assertEquals(ReasonCode.GRANTED_QOS_1.byte, 0x01.toUByte())

    @Test
    fun GrantedQos2() = assertEquals(ReasonCode.GRANTED_QOS_2.byte, 0x02.toUByte())

    @Test
    fun DisconnectwithWillMessage() = assertEquals(ReasonCode.DISCONNECT_WITH_WILL_MESSAGE.byte, 0x04.toUByte())

    @Test
    fun NoMatchingSubscribers() = assertEquals(ReasonCode.NO_MATCHING_SUBSCRIBERS.byte, 0x10.toUByte())

    @Test
    fun NoSubscriptionExisted() = assertEquals(ReasonCode.NO_SUBSCRIPTIONS_EXISTED.byte, 0x11.toUByte())

    @Test
    fun ContinueAuthentication() = assertEquals(ReasonCode.CONTINUE_AUTHENTICATION.byte, 0x18.toUByte())

    @Test
    fun Reauthenticate() = assertEquals(ReasonCode.REAUTHENTICATE.byte, 0x19.toUByte())

    @Test
    fun UnspecifiedError() = assertEquals(ReasonCode.UNSPECIFIED_ERROR.byte, 0x80.toUByte())

    @Test
    fun MalformedPacket() = assertEquals(ReasonCode.MALFORMED_PACKET.byte, 0x81.toUByte())

    @Test
    fun ProtocolError() = assertEquals(ReasonCode.PROTOCOL_ERROR.byte, 0x82.toUByte())

    @Test
    fun ImplementationSpecificError() = assertEquals(ReasonCode.IMPLEMENTATION_SPECIFIC_ERROR.byte, 0x83.toUByte())

    @Test
    fun UnsupportedProtocolVersion() = assertEquals(ReasonCode.UNSUPPORTED_PROTOCOL_VERSION.byte, 0x84.toUByte())

    @Test
    fun ClientIdentifierNotValid() = assertEquals(ReasonCode.CLIENT_IDENTIFIER_NOT_VALID.byte, 0x85.toUByte())

    @Test
    fun BadUserNameOrPassword() = assertEquals(ReasonCode.BAD_USER_NAME_OR_PASSWORD.byte, 0x86.toUByte())

    @Test
    fun NotAuthorized() = assertEquals(ReasonCode.NOT_AUTHORIZED.byte, 0x87.toUByte())

    @Test
    fun ServerUnavailable() = assertEquals(ReasonCode.SERVER_UNAVAILABLE.byte, 0x88.toUByte())

    @Test
    fun ServerBusy() = assertEquals(ReasonCode.SERVER_BUSY.byte, 0x89.toUByte())

    @Test
    fun Banned() = assertEquals(ReasonCode.BANNED.byte, 0x8A.toUByte())

    @Test
    fun ServerShuttingDown() = assertEquals(ReasonCode.SERVER_SHUTTING_DOWN.byte, 0x8B.toUByte())

    @Test
    fun BadAuthenticationMethod() = assertEquals(ReasonCode.BAD_AUTHENTICATION_METHOD.byte, 0x8C.toUByte())

    @Test
    fun KeepAliveTimeout() = assertEquals(ReasonCode.KEEP_ALIVE_TIMEOUT.byte, 0x8D.toUByte())

    @Test
    fun SessionTakenOver() = assertEquals(ReasonCode.SESSION_TAKE_OVER.byte, 0x8E.toUByte())

    @Test
    fun TopicFilterInvalid() = assertEquals(ReasonCode.TOPIC_FILTER_INVALID.byte, 0x8F.toUByte())

    @Test
    fun TopicNameInvalid() = assertEquals(ReasonCode.TOPIC_NAME_INVALID.byte, 0x90.toUByte())

    @Test
    fun PacketIdentifierInUse() = assertEquals(ReasonCode.PACKET_IDENTIFIER_IN_USE.byte, 0x91.toUByte())

    @Test
    fun PacketIdentifierNotFound() = assertEquals(ReasonCode.PACKET_IDENTIFIER_NOT_FOUND.byte, 0x92.toUByte())

    @Test
    fun ReceiveMaximumExceeded() = assertEquals(ReasonCode.RECEIVE_MAXIMUM_EXCEEDED.byte, 0x93.toUByte())

    @Test
    fun TopicAliasInvalid() = assertEquals(ReasonCode.TOPIC_ALIAS_INVALID.byte, 0x94.toUByte())

    @Test
    fun PacketTooLarge() = assertEquals(ReasonCode.PACKET_TOO_LARGE.byte, 0x95.toUByte())

    @Test
    fun MessageRateTooHigh() = assertEquals(ReasonCode.MESSAGE_RATE_TOO_HIGH.byte, 0x96.toUByte())

    @Test
    fun QuotaExceeded() = assertEquals(ReasonCode.QUOTA_EXCEEDED.byte, 0x97.toUByte())

    @Test
    fun AdministrativeAction() = assertEquals(ReasonCode.ADMINISTRATIVE_ACTION.byte, 0x98.toUByte())

    @Test
    fun PayloadFormatInvalid() = assertEquals(ReasonCode.PAYLOAD_FORMAT_INVALID.byte, 0x99.toUByte())

    @Test
    fun RetainNotSupported() = assertEquals(ReasonCode.RETAIN_NOT_SUPPORTED.byte, 0x9A.toUByte())

    @Test
    fun QoSNotSupported() = assertEquals(ReasonCode.QOS_NOT_SUPPORTED.byte, 0x9B.toUByte())

    @Test
    fun UseAnotherServer() = assertEquals(ReasonCode.USE_ANOTHER_SERVER.byte, 0x9C.toUByte())

    @Test
    fun ServerMoved() = assertEquals(ReasonCode.SERVER_MOVED.byte, 0x9D.toUByte())

    @Test
    fun SharedSubscriptionsNotSupported() = assertEquals(ReasonCode.SHARED_SUBSCRIPTIONS_NOT_SUPPORTED.byte, 0x9E.toUByte())

    @Test
    fun ConnectionRateExceeded() = assertEquals(ReasonCode.CONNECTION_RATE_EXCEEDED.byte, 0x9F.toUByte())

    @Test
    fun MaximumConnectTime() = assertEquals(ReasonCode.MAXIMUM_CONNECTION_TIME.byte, 0xA0.toUByte())

    @Test
    fun SubscriptionIdentifierNotSupported() = assertEquals(ReasonCode.SUBSCRIPTION_IDENTIFIERS_NOT_SUPPORTED.byte, 0xA1.toUByte())

    @Test
    fun WildcardSubscriptionsNotSupported() = assertEquals(ReasonCode.WILDCARD_SUBSCRIPTIONS_NOT_SUPPORTED.byte, 0xA2.toUByte())
}
