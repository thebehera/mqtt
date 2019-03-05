@file:Suppress("EXPERIMENTAL_API_USAGE")

package mqtt.wire.control.packet.format.fixed

import mqtt.wire.*
import mqtt.wire.data.QualityOfService.*
import kotlin.test.Test
import kotlin.test.assertEquals

class FlagTests {
    val packetIdentifier = 1.toUShort()

    val controlPacketSpectMatchError = "doesn't match the spec from " +
            "https://docs.oasis-open.org/mqtt/mqtt/v5.0/cos02/mqtt-v5.0-cos02.html#_Toc1477323"

    // Control packet types flagBits.matchesEmptyBits() matching spec
    @Test
    fun controlPacketFlagsMatchSpecForCONNECT() =
            assertEquals(ConnectionRequest().flagBits, emptyFlagBits, controlPacketSpectMatchError)

    @Test
    fun byte1CONNECT() =
            assertEquals(ConnectionRequest().flagBits, emptyFlagBits, controlPacketSpectMatchError)

    @Test
    fun controlPacketFlagsMatchSpecForCONNACK() =
            assertEquals(ConnectionAcknowledgment.flagBits, emptyFlagBits, controlPacketSpectMatchError)

    @Test
    fun controlPacketFlagsMatchSpecForPUBLISH_dup_false_Qos_AtMostOnce_Retain_false() {
        val detailed = PublishMessage()
        assertEquals(detailed.controlPacketValue, 0x00.toUByte(), "invalid byte controlPacketValue")
        assertEquals(detailed.flagBits, emptyFlagBits, controlPacketSpectMatchError)
    }


    @Test
    fun controlPacketFlagsMatchSpecForPUBLISH_dup_trueQos_AtMostOnceRetain_false() {
        val expected = FlagBits(bit3 = true)
        val detailed = PublishMessage(dup = true, qos = AT_MOST_ONCE, retain = false)
        assertEquals(detailed.controlPacketValue, 0x08.toUByte(), "invalid byte controlPacketValue")
        assertEquals(expected, detailed.flagBits, controlPacketSpectMatchError)
        val simple = PublishMessage(dup = true)
        assertEquals(expected, simple.flagBits, controlPacketSpectMatchError)
    }

    @Test
    fun controlPacketFlagsMatchSpecForPUBLISH_dup_false_Qos_AtMostOnce_retain_true() {
        val expected = FlagBits(bit0 = true)
        val detailed = PublishMessage(dup = false, qos = AT_MOST_ONCE, retain = true)
        assertEquals(detailed.controlPacketValue, 0x01.toUByte(), "invalid byte controlPacketValue")
        assertEquals(expected, detailed.flagBits, controlPacketSpectMatchError)
        val simple = PublishMessage(retain = true)
        assertEquals(expected, simple.flagBits, controlPacketSpectMatchError)
    }

    @Test
    fun controlPacketFlagsMatchSpecForPUBLISH_dup_true_Qos_AtMostOnce_retain_true() {
        val expected = FlagBits(bit3 = true, bit0 = true)
        val detailed = PublishMessage(dup = true, qos = AT_MOST_ONCE, retain = true)
        assertEquals(detailed.controlPacketValue, 0x09.toUByte(), "invalid byte controlPacketValue")
        assertEquals(expected, detailed.flagBits, controlPacketSpectMatchError)
        val simple = PublishMessage(dup = true, retain = true)
        assertEquals(expected, simple.flagBits, controlPacketSpectMatchError)
    }

    @Test
    fun controlPacketFlagsMatchSpecForPUBLISH_dup_false_Qos_AtLeastOnce_retain_false() {
        val expected = bit1TrueFlagBits
        val detailed = PublishMessage(dup = false, qos = AT_LEAST_ONCE, retain = false)
        assertEquals(detailed.controlPacketValue, 0x02.toUByte(), "invalid byte controlPacketValue")
        assertEquals(expected, detailed.flagBits, controlPacketSpectMatchError)
        val simple = PublishMessage(qos = AT_LEAST_ONCE)
        assertEquals(expected, simple.flagBits, controlPacketSpectMatchError)
    }

    @Test
    fun controlPacketFlagsMatchSpecForPUBLISH_dup_true_Qos_AtLeastOnce_retain_false() {
        val expected = FlagBits(bit3 = true, bit1 = true)
        val detailed = PublishMessage(dup = true, qos = AT_LEAST_ONCE, retain = false)
        assertEquals(detailed.controlPacketValue, 0x0A.toUByte(), "invalid byte controlPacketValue")
        assertEquals(expected, detailed.flagBits, controlPacketSpectMatchError)
        val simple = PublishMessage(dup = true, qos = AT_LEAST_ONCE)
        assertEquals(expected, simple.flagBits, controlPacketSpectMatchError)
    }

    @Test
    fun controlPacketFlagsMatchSpecForPUBLISH_dup_false_Qos_AtLeastOnce_retain_true() {
        val expected = FlagBits(bit1 = true, bit0 = true)
        val detailed = PublishMessage(dup = false, qos = AT_LEAST_ONCE, retain = true)
        assertEquals(detailed.controlPacketValue, 0x03.toUByte(), "invalid byte controlPacketValue")
        assertEquals(expected, detailed.flagBits, controlPacketSpectMatchError)
        val simple = PublishMessage(qos = AT_LEAST_ONCE, retain = true)
        assertEquals(expected, simple.flagBits, controlPacketSpectMatchError)
    }

    @Test
    fun controlPacketFlagsMatchSpecForPUBLISH_dup_true_Qos_AtLeastOnce_retain_true() {
        val expected = FlagBits(bit3 = true, bit1 = true, bit0 = true)
        val simple = PublishMessage(dup = true, qos = AT_LEAST_ONCE, retain = true)
        assertEquals(simple.controlPacketValue, 0x0B.toUByte(), "invalid byte controlPacketValue")
        assertEquals(expected, simple.flagBits, controlPacketSpectMatchError)
    }

    @Test
    fun controlPacketFlagsMatchSpecForPUBLISH_dup_false_Qos_ExactlyOnce_retain_false() {
        val expected = FlagBits(bit2 = true)
        val detailed = PublishMessage(dup = false, qos = EXACTLY_ONCE, retain = false)
        assertEquals(detailed.controlPacketValue, 0x04.toUByte(), "invalid byte controlPacketValue")
        assertEquals(expected, detailed.flagBits, controlPacketSpectMatchError)
        val simple = PublishMessage(qos = EXACTLY_ONCE)
        assertEquals(expected, simple.flagBits, controlPacketSpectMatchError)
    }

    @Test
    fun controlPacketFlagsMatchSpecForPUBLISH_dup_true_Qos_ExactlyOnce_retain_false() {
        val expected = FlagBits(bit3 = true, bit2 = true)
        val detailed = PublishMessage(dup = true, qos = EXACTLY_ONCE, retain = false)
        assertEquals(detailed.controlPacketValue, 0x0C.toUByte(), "invalid byte controlPacketValue")
        assertEquals(expected, detailed.flagBits, controlPacketSpectMatchError)
        val simple = PublishMessage(dup = true, qos = EXACTLY_ONCE)
        assertEquals(expected, simple.flagBits, controlPacketSpectMatchError)
    }

    @Test
    fun controlPacketFlagsMatchSpecForPUBLISH_dup_false_Qos_ExactlyOnce_retain_true() {
        val expected = FlagBits(bit2 = true, bit0 = true)
        val detailed = PublishMessage(dup = false, qos = EXACTLY_ONCE, retain = true)
        assertEquals(detailed.controlPacketValue, 0x05.toUByte(), "invalid byte controlPacketValue")
        assertEquals(expected, detailed.flagBits, controlPacketSpectMatchError)
        val simple = PublishMessage(qos = EXACTLY_ONCE, retain = true)
        assertEquals(expected, simple.flagBits, controlPacketSpectMatchError)
    }

    @Test
    fun controlPacketFlagsMatchSpecForPUBLISH_dup_true_Qos_ExactlyOnce_retain_true() {
        val expected = FlagBits(bit3 = true, bit2 = true, bit0 = true)
        val simple = PublishMessage(dup = true, qos = EXACTLY_ONCE, retain = true)
        assertEquals(simple.controlPacketValue, 0x0D.toUByte(), "invalid byte controlPacketValue")
        assertEquals(expected, simple.flagBits, controlPacketSpectMatchError)
    }

    @Test
    fun controlPacketFlagsMatchSpecForPUBACK() =
            assertEquals(PublishAcknowledgment(packetIdentifier).flagBits, emptyFlagBits, controlPacketSpectMatchError)

    @Test
    fun controlPacketFlagsMatchSpecForPUBREC() =
            assertEquals(PublishReceived(packetIdentifier).flagBits, emptyFlagBits, controlPacketSpectMatchError)

    @Test
    fun controlPacketFlagsMatchSpecForPUBREL() =
            assertEquals(bit1TrueFlagBits, PublishRelease(packetIdentifier).flagBits, controlPacketSpectMatchError)

    @Test
    fun controlPacketFlagsMatchSpecForPUBCOMP() =
            assertEquals(PublishComplete(packetIdentifier).flagBits, emptyFlagBits, controlPacketSpectMatchError)

    @Test
    fun controlPacketFlagsMatchSpecForSUBSCRIBE() =
            assertEquals(bit1TrueFlagBits, SubscribeRequest(packetIdentifier).flagBits, controlPacketSpectMatchError)

    @Test
    fun controlPacketFlagsMatchSpecForSUBACK() =
            assertEquals(SubscribeAcknowledgment(packetIdentifier).flagBits, emptyFlagBits, controlPacketSpectMatchError)

    @Test
    fun controlPacketFlagsMatchSpecForUNSUBSCRIBE() =
            assertEquals(bit1TrueFlagBits, UnsubscribeAcknowledgment(packetIdentifier).flagBits, controlPacketSpectMatchError)

    @Test
    fun controlPacketFlagsMatchSpecForUNSUBACK() =
            assertEquals(UnsubscribeAcknowledgment(packetIdentifier).flagBits, emptyFlagBits, controlPacketSpectMatchError)

    @Test
    fun controlPacketFlagsMatchSpecForPINGREQ() =
            assertEquals(PingRequest.flagBits, emptyFlagBits, controlPacketSpectMatchError)

    @Test
    fun controlPacketFlagsMatchSpecForPINGRESP() =
            assertEquals(PingResponse.flagBits, emptyFlagBits, controlPacketSpectMatchError)

    @Test
    fun controlPacketFlagsMatchSpecForDISCONNECT() =
            assertEquals(DisconnectNotification.flagBits, emptyFlagBits, controlPacketSpectMatchError)

    @Test
    fun controlPacketFlagsMatchSpecForAUTH() =
            assertEquals(AuthenticationExchange.flagBits, emptyFlagBits, controlPacketSpectMatchError)

    @Test
    fun emptyFlagBitsTest() {
        assertEquals(emptyFlagBits.toByte(), 0x00)
    }

    @Test
    fun bit1TrueFlagBitsTest() {
        assertEquals(bit1TrueFlagBits.toByte(), 0x02)
    }
}
