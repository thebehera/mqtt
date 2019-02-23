package mqtt.wire.control.packet.variable

import mqtt.wire.control.packet.fixed.ControlPacketType.*
import mqtt.wire.control.packet.variable.VariableHeader.Companion.requiresPacketIdentifier
import mqtt.wire.data.QualityOfService
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class VariableHeaderTests {
    val variableHeaderPacketReq = "doesn't match the spec from " +
            "https://docs.oasis-open.org/mqtt/mqtt/v5.0/cos02/mqtt-v5.0-cos02.html#_Toc1477326"

    // Control packet types value matching spec
    @Test
    fun variableHeaderPacketIdentifierRequirementForCONNECT() =
            assertFalse(requiresPacketIdentifier(CONNECT), variableHeaderPacketReq)

    @Test
    fun variableHeaderPacketIdentifierRequirementForCONNACK() =
            assertFalse(requiresPacketIdentifier(CONNACK), variableHeaderPacketReq)

    @Test
    fun variableHeaderPacketIdentifierRequirementForPUBLISH_QoS_AtMostOnce() =
            assertFalse(requiresPacketIdentifier(PUBLISH), variableHeaderPacketReq)

    @Test
    fun variableHeaderPacketIdentifierRequirementForPUBLISH_QoS_AtLeastOnce() =
            assertTrue(requiresPacketIdentifier(PUBLISH, QualityOfService.AT_LEAST_ONCE), variableHeaderPacketReq)

    @Test
    fun variableHeaderPacketIdentifierRequirementForPUBLISH_QoS_ExactlyOnce() =
            assertTrue(requiresPacketIdentifier(PUBLISH, QualityOfService.EXACTLY_ONCE), variableHeaderPacketReq)

    @Test
    fun variableHeaderPacketIdentifierRequirementForPUBACK() =
            assertTrue(requiresPacketIdentifier(PUBACK), variableHeaderPacketReq)

    @Test
    fun variableHeaderPacketIdentifierRequirementForPUBREC() =
            assertTrue(requiresPacketIdentifier(PUBREC), variableHeaderPacketReq)

    @Test
    fun variableHeaderPacketIdentifierRequirementForPUBREL() =
            assertTrue(requiresPacketIdentifier(PUBREL), variableHeaderPacketReq)

    @Test
    fun variableHeaderPacketIdentifierRequirementForPUBCOMP() =
            assertTrue(requiresPacketIdentifier(PUBCOMP), variableHeaderPacketReq)

    @Test
    fun variableHeaderPacketIdentifierRequirementForSUBSCRIBE() =
            assertTrue(requiresPacketIdentifier(SUBSCRIBE), variableHeaderPacketReq)

    @Test
    fun variableHeaderPacketIdentifierRequirementForSUBACK() =
            assertTrue(requiresPacketIdentifier(SUBACK), variableHeaderPacketReq)

    @Test
    fun variableHeaderPacketIdentifierRequirementForUNSUBSCRIBE() =
            assertTrue(requiresPacketIdentifier(UNSUBSCRIBE), variableHeaderPacketReq)

    @Test
    fun variableHeaderPacketIdentifierRequirementForUNSUBACK() =
            assertTrue(requiresPacketIdentifier(UNSUBACK), variableHeaderPacketReq)

    @Test
    fun variableHeaderPacketIdentifierRequirementForPINGREQ() =
            assertFalse(requiresPacketIdentifier(PINGREQ), variableHeaderPacketReq)

    @Test
    fun variableHeaderPacketIdentifierRequirementForPINGRESP() =
            assertFalse(requiresPacketIdentifier(PINGRESP), variableHeaderPacketReq)

    @Test
    fun variableHeaderPacketIdentifierRequirementForDISCONNECT() =
            assertFalse(requiresPacketIdentifier(DISCONNECT), variableHeaderPacketReq)

    @Test
    fun variableHeaderPacketIdentifierRequirementForAUTH() =
            assertFalse(requiresPacketIdentifier(AUTH), variableHeaderPacketReq)
}
