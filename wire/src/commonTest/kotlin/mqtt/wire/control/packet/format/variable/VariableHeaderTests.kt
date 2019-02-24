package mqtt.wire.control.packet.format.variable

import mqtt.wire.control.packet.format.fixed.ControlPacketType.*
import mqtt.wire.data.QualityOfService.AT_LEAST_ONCE
import mqtt.wire.data.QualityOfService.EXACTLY_ONCE
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class VariableHeaderTests {
    val variableHeaderPacketReq = "doesn't match the spec from " +
            "https://docs.oasis-open.org/mqtt/mqtt/v5.0/cos02/mqtt-v5.0-cos02.html#_Toc1477326"

    // Control packet types value matching spec
    @Test
    fun variableHeaderPacketIdentifierRequirementForCONNECT() =
            assertFalse(CONNECT.requiresPacketIdentifier(), variableHeaderPacketReq)

    @Test
    fun variableHeaderPacketIdentifierRequirementForCONNACK() =
            assertFalse(CONNACK.requiresPacketIdentifier(), variableHeaderPacketReq)

    @Test
    fun variableHeaderPacketIdentifierRequirementForPUBLISH_QoS_AtMostOnce() =
            assertFalse(PUBLISH.requiresPacketIdentifier(), variableHeaderPacketReq)

    @Test
    fun variableHeaderPacketIdentifierRequirementForPUBLISH_QoS_AtLeastOnce() =
            assertTrue(PUBLISH.requiresPacketIdentifier(AT_LEAST_ONCE), variableHeaderPacketReq)

    @Test
    fun variableHeaderPacketIdentifierRequirementForPUBLISH_QoS_ExactlyOnce() =
            assertTrue(PUBLISH.requiresPacketIdentifier(EXACTLY_ONCE), variableHeaderPacketReq)

    @Test
    fun variableHeaderPacketIdentifierRequirementForPUBACK() =
            assertTrue(PUBACK.requiresPacketIdentifier(), variableHeaderPacketReq)

    @Test
    fun variableHeaderPacketIdentifierRequirementForPUBREC() =
            assertTrue(PUBREC.requiresPacketIdentifier(), variableHeaderPacketReq)

    @Test
    fun variableHeaderPacketIdentifierRequirementForPUBREL() =
            assertTrue(PUBREL.requiresPacketIdentifier(), variableHeaderPacketReq)

    @Test
    fun variableHeaderPacketIdentifierRequirementForPUBCOMP() =
            assertTrue(PUBCOMP.requiresPacketIdentifier(), variableHeaderPacketReq)

    @Test
    fun variableHeaderPacketIdentifierRequirementForSUBSCRIBE() =
            assertTrue(SUBSCRIBE.requiresPacketIdentifier(), variableHeaderPacketReq)

    @Test
    fun variableHeaderPacketIdentifierRequirementForSUBACK() =
            assertTrue(SUBACK.requiresPacketIdentifier(), variableHeaderPacketReq)

    @Test
    fun variableHeaderPacketIdentifierRequirementForUNSUBSCRIBE() =
            assertTrue(UNSUBSCRIBE.requiresPacketIdentifier(), variableHeaderPacketReq)

    @Test
    fun variableHeaderPacketIdentifierRequirementForUNSUBACK() =
            assertTrue(UNSUBACK.requiresPacketIdentifier(), variableHeaderPacketReq)

    @Test
    fun variableHeaderPacketIdentifierRequirementForPINGREQ() =
            assertFalse(PINGREQ.requiresPacketIdentifier(), variableHeaderPacketReq)

    @Test
    fun variableHeaderPacketIdentifierRequirementForPINGRESP() =
            assertFalse(PINGRESP.requiresPacketIdentifier(), variableHeaderPacketReq)

    @Test
    fun variableHeaderPacketIdentifierRequirementForDISCONNECT() =
            assertFalse(DISCONNECT.requiresPacketIdentifier(), variableHeaderPacketReq)

    @Test
    fun variableHeaderPacketIdentifierRequirementForAUTH() =
            assertFalse(AUTH.requiresPacketIdentifier(), variableHeaderPacketReq)
}
