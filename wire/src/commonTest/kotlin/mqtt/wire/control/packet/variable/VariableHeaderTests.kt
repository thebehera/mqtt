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
    fun `Variable Header packet identifier requirement for CONNECT`() =
            assertFalse(requiresPacketIdentifier(CONNECT), variableHeaderPacketReq)

    @Test
    fun `Variable Header packet identifier requirement for CONNACK`() =
            assertFalse(requiresPacketIdentifier(CONNACK), variableHeaderPacketReq)

    @Test
    fun `Variable Header packet identifier requirement for PUBLISH QoS=AtMostOnce`() =
            assertFalse(requiresPacketIdentifier(PUBLISH), variableHeaderPacketReq)

    @Test
    fun `Variable Header packet identifier requirement for PUBLISH QoS=AtLeastOnce A PUBLISH packet MUST NOT contain a Packet Identifier if its QoS value is set to 0`() =
            assertTrue(requiresPacketIdentifier(PUBLISH, QualityOfService.AT_LEAST_ONCE), variableHeaderPacketReq)

    @Test
    fun `Variable Header packet identifier requirement for PUBLISH QoS=ExactlyOnce A PUBLISH packet MUST NOT contain a Packet Identifier if its QoS value is set to 0`() =
            assertTrue(requiresPacketIdentifier(PUBLISH, QualityOfService.EXACTLY_ONCE), variableHeaderPacketReq)

    @Test
    fun `Variable Header packet identifier requirement for PUBACK`() =
            assertTrue(requiresPacketIdentifier(PUBACK), variableHeaderPacketReq)

    @Test
    fun `Variable Header packet identifier requirement for PUBREC`() =
            assertTrue(requiresPacketIdentifier(PUBREC), variableHeaderPacketReq)

    @Test
    fun `Variable Header packet identifier requirement for PUBREL`() =
            assertTrue(requiresPacketIdentifier(PUBREL), variableHeaderPacketReq)

    @Test
    fun `Variable Header packet identifier requirement for PUBCOMP`() =
            assertTrue(requiresPacketIdentifier(PUBCOMP), variableHeaderPacketReq)

    @Test
    fun `Variable Header packet identifier requirement for SUBSCRIBE`() =
            assertTrue(requiresPacketIdentifier(SUBSCRIBE), variableHeaderPacketReq)

    @Test
    fun `Variable Header packet identifier requirement for SUBACK`() =
            assertTrue(requiresPacketIdentifier(SUBACK), variableHeaderPacketReq)

    @Test
    fun `Variable Header packet identifier requirement for UNSUBSCRIBE`() =
            assertTrue(requiresPacketIdentifier(UNSUBSCRIBE), variableHeaderPacketReq)

    @Test
    fun `Variable Header packet identifier requirement for UNSUBACK`() =
            assertTrue(requiresPacketIdentifier(UNSUBACK), variableHeaderPacketReq)

    @Test
    fun `Variable Header packet identifier requirement for PINGREQ`() =
            assertFalse(requiresPacketIdentifier(PINGREQ), variableHeaderPacketReq)

    @Test
    fun `Variable Header packet identifier requirement for PINGRESP`() =
            assertFalse(requiresPacketIdentifier(PINGRESP), variableHeaderPacketReq)

    @Test
    fun `Variable Header packet identifier requirement for DISCONNECT`() =
            assertFalse(requiresPacketIdentifier(DISCONNECT), variableHeaderPacketReq)

    @Test
    fun `Variable Header packet identifier requirement for AUTH`() =
            assertFalse(requiresPacketIdentifier(AUTH), variableHeaderPacketReq)
}
