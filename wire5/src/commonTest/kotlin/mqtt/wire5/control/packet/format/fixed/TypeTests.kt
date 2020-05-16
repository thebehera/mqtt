@file:Suppress("EXPERIMENTAL_API_USAGE")

package mqtt.wire5.control.packet.format.fixed

import mqtt.wire.control.packet.format.ReasonCode.*
import mqtt.wire.control.packet.format.fixed.DirectionOfFlow.*
import mqtt.wire.data.topic.Filter
import mqtt.wire5.control.packet.*
import mqtt.wire5.control.packet.PublishMessage.VariableHeader
import kotlin.test.Test
import kotlin.test.assertEquals

class TypeTests {
    private val packetIdentifier = 0

    private val controlPacketSpectMatchError = "doesn't match the spec from " +
            "https://docs.oasis-open.org/mqtt/mqtt/v5.0/cos02/mqtt-v5.0-cos02.html#_Toc1477322"

    // Control packet types controlPacketValue matching spec
    @Test
    fun controlPacketTypeValueMatchesSpecForCONNECT() =
            assertEquals(1, ConnectionRequest().controlPacketValue, controlPacketSpectMatchError)

    @Test
    fun controlPacketTypeValueMatchesSpecForCONNACK() =
            assertEquals(2, ConnectionAcknowledgment().controlPacketValue, controlPacketSpectMatchError)

    @Test
    fun controlPacketTypeValueMatchesSpecForPUBLISH() {
        val variable = VariableHeader("t")
        assertEquals(3, PublishMessage(variable = variable).controlPacketValue, controlPacketSpectMatchError)
    }

    @Test
    fun controlPacketTypeValueMatchesSpecForPUBACK() =
            assertEquals(4, PublishAcknowledgment(PublishAcknowledgment.VariableHeader(packetIdentifier)).controlPacketValue, controlPacketSpectMatchError)

    @Test
    fun controlPacketTypeValueMatchesSpecForPUBREC() =
            assertEquals(5, PublishReceived(PublishReceived.VariableHeader(packetIdentifier)).controlPacketValue, controlPacketSpectMatchError)

    @Test
    fun controlPacketTypeValueMatchesSpecForPUBREL() =
            assertEquals(6, PublishRelease(PublishRelease.VariableHeader(packetIdentifier)).controlPacketValue, controlPacketSpectMatchError)

    @Test
    fun controlPacketTypeValueMatchesSpecForPUBCOMP() =
            assertEquals(7, PublishComplete(PublishComplete.VariableHeader(packetIdentifier)).controlPacketValue, controlPacketSpectMatchError)

    @Test
    fun controlPacketTypeValueMatchesSpecForSUBSCRIBE() =
            assertEquals(8, SubscribeRequest(SubscribeRequest.VariableHeader(packetIdentifier), setOf(Subscription(Filter("yolo")))).controlPacketValue, controlPacketSpectMatchError)

    @Test
    fun controlPacketTypeValueMatchesSpecForSUBACK() =
            assertEquals(9, SubscribeAcknowledgement(packetIdentifier.toUShort(), GRANTED_QOS_0).controlPacketValue, controlPacketSpectMatchError)

    @Test
    fun controlPacketTypeValueMatchesSpecForUNSUBSCRIBE() =
        assertEquals(
            10,
            UnsubscribeRequest(
                UnsubscribeRequest.VariableHeader(packetIdentifier),
                setOf("yolo")
            ).controlPacketValue,
            controlPacketSpectMatchError
        )

    @Test
    fun controlPacketTypeValueMatchesSpecForUNSUBACK() =
            assertEquals(11, UnsubscribeAcknowledgment(UnsubscribeAcknowledgment.VariableHeader(packetIdentifier), listOf(GRANTED_QOS_1)).controlPacketValue, controlPacketSpectMatchError)

    @Test
    fun controlPacketTypeValueMatchesSpecForPINGREQ() =
            assertEquals(12, PingRequest.controlPacketValue, controlPacketSpectMatchError)

    @Test
    fun controlPacketTypeValueMatchesSpecForPINGRESP() =
            assertEquals(13, PingResponse.controlPacketValue, controlPacketSpectMatchError)

    @Test
    fun controlPacketTypeValueMatchesSpecForDISCONNECT() =
            assertEquals(14, DisconnectNotification(DisconnectNotification.VariableHeader(NORMAL_DISCONNECTION)).controlPacketValue, controlPacketSpectMatchError)

    @Test
    fun controlPacketTypeValueMatchesSpecForAUTH() =
        assertEquals(
            15,
            AuthenticationExchange(
                AuthenticationExchange.VariableHeader(
                    SUCCESS,
                    AuthenticationExchange.VariableHeader.Properties("yolo")
                )
            ).controlPacketValue,
            controlPacketSpectMatchError
        )


    // Control packet types direction of flow matching spec
    @Test
    fun controlPacketTypeDirectionOfFlowCONNECT() =
            assertEquals(CLIENT_TO_SERVER, ConnectionRequest().direction, controlPacketSpectMatchError)

    @Test
    fun controlPacketTypeDirectionOfFlowCONNACK() =
            assertEquals(SERVER_TO_CLIENT, ConnectionAcknowledgment().direction, controlPacketSpectMatchError)

    @Test
    fun controlPacketTypeDirectionOfFlowPUBLISH() {
        val variable = VariableHeader("t")
        assertEquals(BIDIRECTIONAL, PublishMessage(variable = variable).direction, controlPacketSpectMatchError)
    }

    @Test
    fun controlPacketTypeDirectionOfFlowPUBACK() =
            assertEquals(BIDIRECTIONAL, PublishAcknowledgment(PublishAcknowledgment.VariableHeader(packetIdentifier)).direction, controlPacketSpectMatchError)

    @Test
    fun controlPacketTypeDirectionOfFlowPUBREC() =
            assertEquals(BIDIRECTIONAL, PublishReceived(PublishReceived.VariableHeader(packetIdentifier)).direction, controlPacketSpectMatchError)

    @Test
    fun controlPacketTypeDirectionOfFlowPUBREL() =
            assertEquals(BIDIRECTIONAL, PublishRelease(PublishRelease.VariableHeader(packetIdentifier)).direction, controlPacketSpectMatchError)

    @Test
    fun controlPacketTypeDirectionOfFlowPUBCOMP() =
            assertEquals(BIDIRECTIONAL, PublishComplete(PublishComplete.VariableHeader(packetIdentifier)).direction, controlPacketSpectMatchError)

    @Test
    fun controlPacketTypeDirectionOfFlowSUBSCRIBE() =
            assertEquals(CLIENT_TO_SERVER, SubscribeRequest(SubscribeRequest.VariableHeader(packetIdentifier), setOf(Subscription(Filter("yolo")))).direction, controlPacketSpectMatchError)

    @Test
    fun controlPacketTypeDirectionOfFlowSUBACK() =
            assertEquals(SERVER_TO_CLIENT, SubscribeAcknowledgement(packetIdentifier.toUShort(), GRANTED_QOS_0).direction, controlPacketSpectMatchError)

    @Test
    fun controlPacketTypeDirectionOfFlowUNSUBSCRIBE() =
        assertEquals(
            CLIENT_TO_SERVER,
            UnsubscribeRequest(UnsubscribeRequest.VariableHeader(packetIdentifier), setOf("yolo")).direction,
            controlPacketSpectMatchError
        )

    @Test
    fun controlPacketTypeDirectionOfFlowUNSUBACK() =
            assertEquals(SERVER_TO_CLIENT, UnsubscribeAcknowledgment(UnsubscribeAcknowledgment.VariableHeader(packetIdentifier), listOf(GRANTED_QOS_1)).direction, controlPacketSpectMatchError)

    @Test
    fun controlPacketTypeDirectionOfFlowPINGREQ() =
            assertEquals(CLIENT_TO_SERVER, PingRequest.direction, controlPacketSpectMatchError)

    @Test
    fun controlPacketTypeDirectionOfFlowPINGRESP() =
            assertEquals(SERVER_TO_CLIENT, PingResponse.direction, controlPacketSpectMatchError)

    @Test
    fun controlPacketTypeDirectionOfFlowDISCONNECT() =
            assertEquals(BIDIRECTIONAL, DisconnectNotification(DisconnectNotification.VariableHeader(NORMAL_DISCONNECTION)).direction, controlPacketSpectMatchError)

    @Test
    fun controlPacketTypeDirectionOfFlowAUTH() =
        assertEquals(
            BIDIRECTIONAL,
            AuthenticationExchange(
                AuthenticationExchange.VariableHeader(
                    SUCCESS,
                    AuthenticationExchange.VariableHeader.Properties("yolo")
                )
            ).direction,
            controlPacketSpectMatchError
        )
}
