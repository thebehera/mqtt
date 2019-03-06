@file:Suppress("EXPERIMENTAL_API_USAGE")

package mqtt.wire.control.packet.format.fixed

import mqtt.wire.control.packet.*
import mqtt.wire.data.QualityOfService.*
import kotlin.test.Test
import kotlin.test.assertEquals

class FlagTests {
    private val packetIdentifier = 1.toUShort()

    private val controlPacketSpectMatchError = "doesn't match the spec from " +
            "https://docs.oasis-open.org/mqtt/mqtt/v5.0/cos02/mqtt-v5.0-cos02.html#_Toc1477323"

    // Control packet types flagBits.matchesEmptyBits() matching spec
    @Test
    fun controlPacketFlagsMatchSpecForCONNECT() =
            assertEquals(ConnectionRequest().flags, 0b0, controlPacketSpectMatchError)

    @Test
    fun byte1CONNECT() =
            assertEquals(ConnectionRequest().flags, 0b0, controlPacketSpectMatchError)

    @Test
    fun controlPacketFlagsMatchSpecForCONNACK() =
            assertEquals(ConnectionAcknowledgment.flags, 0b0, controlPacketSpectMatchError)

    @Test
    fun controlPacketFlagsMatchSpecForPUBLISH_dup_false_Qos_AtMostOnce_Retain_false() {
        val detailed = PublishMessage()
        assertEquals(detailed.controlPacketValue, 0x03, "invalid byte controlPacketValue")
        assertEquals(detailed.flags, 0b0, controlPacketSpectMatchError)
    }


    @Test
    fun controlPacketFlagsMatchSpecForPUBLISH_dup_trueQos_AtMostOnceRetain_false() {

        val x = ConnectionRequest()
        x.variableHeader.keepAliveSeconds
        val expected = 0b1000.toByte()
        val detailed = PublishMessage(dup = true, qos = AT_MOST_ONCE, retain = false)
        assertEquals(detailed.controlPacketValue, 0x03, "invalid byte controlPacketValue")
        assertEquals(expected, detailed.flags, controlPacketSpectMatchError)
        val simple = PublishMessage(dup = true)
        assertEquals(expected, simple.flags, controlPacketSpectMatchError)
    }

    @Test
    fun controlPacketFlagsMatchSpecForPUBLISH_dup_false_Qos_AtMostOnce_retain_true() {
        val expected = 0b1.toByte()
        val detailed = PublishMessage(dup = false, qos = AT_MOST_ONCE, retain = true)
        assertEquals(detailed.controlPacketValue, 0x03, "invalid byte controlPacketValue")
        assertEquals(expected, detailed.flags, controlPacketSpectMatchError)
        val simple = PublishMessage(retain = true)
        assertEquals(expected, simple.flags, controlPacketSpectMatchError)
    }

    @Test
    fun controlPacketFlagsMatchSpecForPUBLISH_dup_true_Qos_AtMostOnce_retain_true() {
        val expected = 0b1001.toByte()
        val detailed = PublishMessage(dup = true, qos = AT_MOST_ONCE, retain = true)
        assertEquals(detailed.controlPacketValue, 0x03, "invalid byte controlPacketValue")
        assertEquals(expected, detailed.flags, controlPacketSpectMatchError)
        val simple = PublishMessage(dup = true, retain = true)
        assertEquals(expected, simple.flags, controlPacketSpectMatchError)
    }

    @Test // THIS IS WHAT I NEED TO WORK ON FIRST FIX THIS
    fun controlPacketFlagsMatchSpecForPUBLISH_dup_false_Qos_AtLeastOnce_retain_false() {
        val expected = 0b10.toByte()
        val detailed = PublishMessage(packetIdentifier = packetIdentifier, dup = false, qos = AT_LEAST_ONCE,
                retain = false)
        assertEquals(detailed.controlPacketValue, 0x03,
                "Invalid Byte 1 in the fixed header: Control Packet Value")
        val byteAsUInt = detailed.serialize[0].toUInt()

        assertEquals(byteAsUInt.shr(4), 0x03.toUInt(),
                "Invalid Byte 1 in the fixed header: Control Packet Value serialize shift right 4 times")
        val expectedFlagMatch = byteAsUInt.shl(4).toByte().toInt().shr(4).toByte()
        assertEquals(expectedFlagMatch, 0b0010,
                "Invalid Byte 1 in the fixed header: Flags dont match")
        assertEquals(expected, detailed.flags, controlPacketSpectMatchError)
        val simple = PublishMessage(packetIdentifier = packetIdentifier, qos = AT_LEAST_ONCE)
        assertEquals(expected, simple.flags, controlPacketSpectMatchError)
    }

    @Test
    fun controlPacketFlagsMatchSpecForPUBLISH_dup_true_Qos_AtLeastOnce_retain_false() {
        val expected = 0b1010.toByte()
        val detailed = PublishMessage(packetIdentifier = packetIdentifier, dup = true, qos = AT_LEAST_ONCE, retain = false)
        assertEquals(detailed.controlPacketValue, 0x03, "invalid byte controlPacketValue")
        assertEquals(expected, detailed.flags, controlPacketSpectMatchError)
        val simple = PublishMessage(packetIdentifier = packetIdentifier, dup = true, qos = AT_LEAST_ONCE)
        assertEquals(expected, simple.flags, controlPacketSpectMatchError)
    }

    @Test
    fun controlPacketFlagsMatchSpecForPUBLISH_dup_false_Qos_AtLeastOnce_retain_true() {
        val expected = 0b11.toByte()
        val detailed = PublishMessage(packetIdentifier = packetIdentifier, dup = false, qos = AT_LEAST_ONCE, retain = true)
        assertEquals(detailed.controlPacketValue, 0x03, "invalid byte controlPacketValue")
        assertEquals(expected, detailed.flags, controlPacketSpectMatchError)
        val simple = PublishMessage(packetIdentifier = packetIdentifier, qos = AT_LEAST_ONCE, retain = true)
        assertEquals(expected, simple.flags, controlPacketSpectMatchError)
    }

    @Test
    fun controlPacketFlagsMatchSpecForPUBLISH_dup_true_Qos_AtLeastOnce_retain_true() {
        val expected = 0b1011.toByte()
        val simple = PublishMessage(packetIdentifier = packetIdentifier, dup = true, qos = AT_LEAST_ONCE, retain = true)
        assertEquals(simple.controlPacketValue, 0x03, "invalid byte controlPacketValue")
        assertEquals(expected, simple.flags, controlPacketSpectMatchError)
    }

    @Test
    fun controlPacketFlagsMatchSpecForPUBLISH_dup_false_Qos_ExactlyOnce_retain_false() {
        val expected = 0b100.toByte()
        val detailed = PublishMessage(packetIdentifier = packetIdentifier, dup = false, qos = EXACTLY_ONCE, retain = false)
        assertEquals(detailed.controlPacketValue, 0x03, "invalid byte controlPacketValue")
        assertEquals(expected, detailed.flags, controlPacketSpectMatchError)
        val simple = PublishMessage(packetIdentifier = packetIdentifier, qos = EXACTLY_ONCE)
        assertEquals(expected, simple.flags, controlPacketSpectMatchError)
    }

    @Test
    fun controlPacketFlagsMatchSpecForPUBLISH_dup_true_Qos_ExactlyOnce_retain_false() {
        val expected = 0b1100.toByte()
        val detailed = PublishMessage(packetIdentifier = packetIdentifier, dup = true, qos = EXACTLY_ONCE, retain = false)
        assertEquals(detailed.controlPacketValue, 0x03, "invalid byte controlPacketValue")
        assertEquals(expected, detailed.flags, controlPacketSpectMatchError)
        val simple = PublishMessage(packetIdentifier = packetIdentifier, dup = true, qos = EXACTLY_ONCE)
        assertEquals(expected, simple.flags, controlPacketSpectMatchError)
    }

    @Test
    fun controlPacketFlagsMatchSpecForPUBLISH_dup_false_Qos_ExactlyOnce_retain_true() {
        val expected = 0b101.toByte()
        val detailed = PublishMessage(packetIdentifier = packetIdentifier, dup = false, qos = EXACTLY_ONCE, retain = true)
        assertEquals(detailed.controlPacketValue, 0x03, "invalid byte controlPacketValue")
        assertEquals(expected, detailed.flags, controlPacketSpectMatchError)
        val simple = PublishMessage(packetIdentifier = packetIdentifier, qos = EXACTLY_ONCE, retain = true)
        assertEquals(expected, simple.flags, controlPacketSpectMatchError)
    }

    @Test
    fun controlPacketFlagsMatchSpecForPUBLISH_dup_true_Qos_ExactlyOnce_retain_true() {
        val expected = 0b1101.toByte()
        val simple = PublishMessage(packetIdentifier = packetIdentifier, dup = true, qos = EXACTLY_ONCE, retain = true)
        assertEquals(simple.controlPacketValue, 0x03, "invalid byte controlPacketValue")
        assertEquals(expected, simple.flags, controlPacketSpectMatchError)
    }

    @Test
    fun controlPacketFlagsMatchSpecForPUBACK() =
            assertEquals(PublishAcknowledgment(packetIdentifier).flags, 0b0, controlPacketSpectMatchError)

    @Test
    fun controlPacketFlagsMatchSpecForPUBREC() =
            assertEquals(PublishReceived(packetIdentifier).flags, 0b0, controlPacketSpectMatchError)

    @Test
    fun controlPacketFlagsMatchSpecForPUBREL() =
            assertEquals(PublishRelease(packetIdentifier).flags, 0b10, controlPacketSpectMatchError)

    @Test
    fun controlPacketFlagsMatchSpecForPUBCOMP() =
            assertEquals(PublishComplete(packetIdentifier).flags, 0b0, controlPacketSpectMatchError)

    @Test
    fun controlPacketFlagsMatchSpecForSUBSCRIBE() =
            assertEquals(0b10, SubscribeRequest(packetIdentifier).flags, controlPacketSpectMatchError)

    @Test
    fun controlPacketFlagsMatchSpecForSUBACK() =
            assertEquals(SubscribeAcknowledgment(packetIdentifier).flags, 0b0, controlPacketSpectMatchError)

    @Test
    fun controlPacketFlagsMatchSpecForUNSUBSCRIBE() =
            assertEquals(UnsubscribeAcknowledgment(packetIdentifier).flags, 0b0, controlPacketSpectMatchError)

    @Test
    fun controlPacketFlagsMatchSpecForUNSUBACK() =
            assertEquals(UnsubscribeAcknowledgment(packetIdentifier).flags, 0b0, controlPacketSpectMatchError)

    @Test
    fun controlPacketFlagsMatchSpecForPINGREQ() =
            assertEquals(PingRequest.flags, 0b0, controlPacketSpectMatchError)

    @Test
    fun controlPacketFlagsMatchSpecForPINGRESP() =
            assertEquals(PingResponse.flags, 0b0, controlPacketSpectMatchError)

    @Test
    fun controlPacketFlagsMatchSpecForDISCONNECT() =
            assertEquals(DisconnectNotification.flags, 0b0, controlPacketSpectMatchError)

    @Test
    fun controlPacketFlagsMatchSpecForAUTH() =
            assertEquals(AuthenticationExchange.flags, 0b0, controlPacketSpectMatchError)

    @Test
    fun emptyFlagBitsTest() {
        assertEquals(0b0, 0x00)
    }

    @Test
    fun bit1TrueFlagBitsTest() {
        assertEquals(0b10, 0x02)
    }
}
