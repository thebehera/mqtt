@file:Suppress("EXPERIMENTAL_API_USAGE")

package mqtt.wire.control.packet.format.fixed

import mqtt.wire.control.packet.*
import mqtt.wire.control.packet.PublishMessage.VariableHeader
import mqtt.wire.control.packet.format.fixed.DirectionOfFlow.*
import mqtt.wire.data.MqttUtf8String
import kotlin.test.Test
import kotlin.test.assertEquals

class TypeTests {
    private val packetIdentifier = 0.toUShort()

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
        val variable = VariableHeader(MqttUtf8String("t"))
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
            assertEquals(8, SubscribeRequest(SubscribeRequest.VariableHeader(packetIdentifier), setOf(Subscription(MqttUtf8String("yolo")))).controlPacketValue, controlPacketSpectMatchError)

    @Test
    fun controlPacketTypeValueMatchesSpecForSUBACK() =
            assertEquals(9, SubscribeAcknowledgment(packetIdentifier).controlPacketValue, controlPacketSpectMatchError)

    @Test
    fun controlPacketTypeValueMatchesSpecForUNSUBSCRIBE() =
            assertEquals(10, UnsubscribeRequest(packetIdentifier).controlPacketValue, controlPacketSpectMatchError)

    @Test
    fun controlPacketTypeValueMatchesSpecForUNSUBACK() =
            assertEquals(11, UnsubscribeAcknowledgment(packetIdentifier).controlPacketValue, controlPacketSpectMatchError)

    @Test
    fun controlPacketTypeValueMatchesSpecForPINGREQ() =
            assertEquals(12, PingRequest.controlPacketValue, controlPacketSpectMatchError)

    @Test
    fun controlPacketTypeValueMatchesSpecForPINGRESP() =
            assertEquals(13, PingResponse.controlPacketValue, controlPacketSpectMatchError)

    @Test
    fun controlPacketTypeValueMatchesSpecForDISCONNECT() =
            assertEquals(14, DisconnectNotification.controlPacketValue, controlPacketSpectMatchError)

    @Test
    fun controlPacketTypeValueMatchesSpecForAUTH() =
            assertEquals(15, AuthenticationExchange.controlPacketValue, controlPacketSpectMatchError)


    // Control packet types direction of flow matching spec
    @Test
    fun controlPacketTypeDirectionOfFlowCONNECT() =
            assertEquals(CLIENT_TO_SERVER, ConnectionRequest().direction, controlPacketSpectMatchError)

    @Test
    fun controlPacketTypeDirectionOfFlowCONNACK() =
            assertEquals(SERVER_TO_CLIENT, ConnectionAcknowledgment().direction, controlPacketSpectMatchError)

    @Test
    fun controlPacketTypeDirectionOfFlowPUBLISH() {
        val variable = VariableHeader(MqttUtf8String("t"))
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
            assertEquals(CLIENT_TO_SERVER, SubscribeRequest(SubscribeRequest.VariableHeader(packetIdentifier), setOf(Subscription(MqttUtf8String("yolo")))).direction, controlPacketSpectMatchError)

    @Test
    fun controlPacketTypeDirectionOfFlowSUBACK() =
            assertEquals(SERVER_TO_CLIENT, SubscribeAcknowledgment(packetIdentifier).direction, controlPacketSpectMatchError)

    @Test
    fun controlPacketTypeDirectionOfFlowUNSUBSCRIBE() =
            assertEquals(CLIENT_TO_SERVER, UnsubscribeRequest(packetIdentifier).direction, controlPacketSpectMatchError)

    @Test
    fun controlPacketTypeDirectionOfFlowUNSUBACK() =
            assertEquals(SERVER_TO_CLIENT, UnsubscribeAcknowledgment(packetIdentifier).direction, controlPacketSpectMatchError)

    @Test
    fun controlPacketTypeDirectionOfFlowPINGREQ() =
            assertEquals(CLIENT_TO_SERVER, PingRequest.direction, controlPacketSpectMatchError)

    @Test
    fun controlPacketTypeDirectionOfFlowPINGRESP() =
            assertEquals(SERVER_TO_CLIENT, PingResponse.direction, controlPacketSpectMatchError)

    @Test
    fun controlPacketTypeDirectionOfFlowDISCONNECT() =
            assertEquals(BIDIRECTIONAL, DisconnectNotification.direction, controlPacketSpectMatchError)

    @Test
    fun controlPacketTypeDirectionOfFlowAUTH() =
            assertEquals(BIDIRECTIONAL, AuthenticationExchange.direction, controlPacketSpectMatchError)
}
