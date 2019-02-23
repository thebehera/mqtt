package mqtt.wire.control.packet.fixed

import mqtt.wire.control.packet.fixed.ControlPacketType.*
import mqtt.wire.data.QualityOfService.*
import kotlin.test.Test
import kotlin.test.assertEquals

class FlagTests {

    val controlPacketSpectMatchError = "doesn't match the spec from " +
            "https://docs.oasis-open.org/mqtt/mqtt/v5.0/cos02/mqtt-v5.0-cos02.html#_Toc1477323"

    // Control packet types flags().matchesEmptyBits() matching spec
    @Test
    fun controlPacketFlagsMatchSpecForCONNECT() {
        assertEquals(emptyFlagBits, CONNECT.flags(), controlPacketSpectMatchError)
    }

    @Test
    fun controlPacketFlagsMatchSpecForCONNACK() =
            assertEquals(emptyFlagBits, CONNACK.flags(), controlPacketSpectMatchError)

    @Test
    fun controlPacketFlagsMatchSpecForPUBLISH_dup_false_Qos_AtMostOnce_Retain_false() {
        val detailed = PUBLISH.flags(dup = false, qos = AT_MOST_ONCE, retain = false)
        assertEquals(detailed.toByte(), 0x00, "invalid byte value")
        assertEquals(emptyFlagBits, detailed, controlPacketSpectMatchError)
        val simple = PUBLISH.flags()
        assertEquals(emptyFlagBits, simple, controlPacketSpectMatchError)
    }

    @Test
    fun controlPacketFlagsMatchSpecForPUBLISH_dup_trueQos_AtMostOnceRetain_false() {
        val expected = FlagBits(bit3 = true)
        val detailed = PUBLISH.flags(dup = true, qos = AT_MOST_ONCE, retain = false)
        assertEquals(detailed.toByte(), 0x08, "invalid byte value")
        assertEquals(expected, detailed, controlPacketSpectMatchError)
        val simple = PUBLISH.flags(dup = true)
        assertEquals(expected, simple, controlPacketSpectMatchError)
    }

    @Test
    fun controlPacketFlagsMatchSpecForPUBLISH_dup_false_Qos_AtMostOnce_retain_true() {
        val expected = FlagBits(bit0 = true)
        val detailed = PUBLISH.flags(dup = false, qos = AT_MOST_ONCE, retain = true)
        assertEquals(detailed.toByte(), 0x01, "invalid byte value")
        assertEquals(expected, detailed, controlPacketSpectMatchError)
        val simple = PUBLISH.flags(retain = true)
        assertEquals(expected, simple, controlPacketSpectMatchError)
    }

    @Test
    fun controlPacketFlagsMatchSpecForPUBLISH_dup_true_Qos_AtMostOnce_retain_true() {
        val expected = FlagBits(bit3 = true, bit0 = true)
        val detailed = PUBLISH.flags(dup = true, qos = AT_MOST_ONCE, retain = true)
        assertEquals(detailed.toByte(), 0x09, "invalid byte value")
        assertEquals(expected, detailed, controlPacketSpectMatchError)
        val simple = PUBLISH.flags(dup = true, retain = true)
        assertEquals(expected, simple, controlPacketSpectMatchError)
    }

    @Test
    fun controlPacketFlagsMatchSpecForPUBLISH_dup_false_Qos_AtLeastOnce_retain_false() {
        val expected = bit1TrueFlagBits
        val detailed = PUBLISH.flags(dup = false, qos = AT_LEAST_ONCE, retain = false)
        assertEquals(detailed.toByte(), 0x02, "invalid byte value")
        assertEquals(expected, detailed, controlPacketSpectMatchError)
        val simple = PUBLISH.flags(qos = AT_LEAST_ONCE)
        assertEquals(expected, simple, controlPacketSpectMatchError)
    }

    @Test
    fun controlPacketFlagsMatchSpecForPUBLISH_dup_true_Qos_AtLeastOnce_retain_false() {
        val expected = FlagBits(bit3 = true, bit1 = true)
        val detailed = PUBLISH.flags(dup = true, qos = AT_LEAST_ONCE, retain = false)
        assertEquals(detailed.toByte(), 0x0A, "invalid byte value")
        assertEquals(expected, detailed, controlPacketSpectMatchError)
        val simple = PUBLISH.flags(dup = true, qos = AT_LEAST_ONCE)
        assertEquals(expected, simple, controlPacketSpectMatchError)
    }

    @Test
    fun controlPacketFlagsMatchSpecForPUBLISH_dup_false_Qos_AtLeastOnce_retain_true() {
        val expected = FlagBits(bit1 = true, bit0 = true)
        val detailed = PUBLISH.flags(dup = false, qos = AT_LEAST_ONCE, retain = true)
        assertEquals(detailed.toByte(), 0x03, "invalid byte value")
        assertEquals(expected, detailed, controlPacketSpectMatchError)
        val simple = PUBLISH.flags(qos = AT_LEAST_ONCE, retain = true)
        assertEquals(expected, simple, controlPacketSpectMatchError)
    }

    @Test
    fun controlPacketFlagsMatchSpecForPUBLISH_dup_true_Qos_AtLeastOnce_retain_true() {
        val expected = FlagBits(bit3 = true, bit1 = true, bit0 = true)
        val simple = PUBLISH.flags(dup = true, qos = AT_LEAST_ONCE, retain = true)
        assertEquals(simple.toByte(), 0x0B, "invalid byte value")
        assertEquals(expected, simple, controlPacketSpectMatchError)
    }

    @Test
    fun controlPacketFlagsMatchSpecForPUBLISH_dup_false_Qos_ExactlyOnce_retain_false() {
        val expected = FlagBits(bit2 = true)
        val detailed = PUBLISH.flags(dup = false, qos = EXACTLY_ONCE, retain = false)
        assertEquals(detailed.toByte(), 0x04, "invalid byte value")
        assertEquals(expected, detailed, controlPacketSpectMatchError)
        val simple = PUBLISH.flags(qos = EXACTLY_ONCE)
        assertEquals(expected, simple, controlPacketSpectMatchError)
    }

    @Test
    fun controlPacketFlagsMatchSpecForPUBLISH_dup_true_Qos_ExactlyOnce_retain_false() {
        val expected = FlagBits(bit3 = true, bit2 = true)
        val detailed = PUBLISH.flags(dup = true, qos = EXACTLY_ONCE, retain = false)
        assertEquals(detailed.toByte(), 0x0C, "invalid byte value")
        assertEquals(expected, detailed, controlPacketSpectMatchError)
        val simple = PUBLISH.flags(dup = true, qos = EXACTLY_ONCE)
        assertEquals(expected, simple, controlPacketSpectMatchError)
    }

    @Test
    fun controlPacketFlagsMatchSpecForPUBLISH_dup_false_Qos_ExactlyOnce_retain_true() {
        val expected = FlagBits(bit2 = true, bit0 = true)
        val detailed = PUBLISH.flags(dup = false, qos = EXACTLY_ONCE, retain = true)
        assertEquals(detailed.toByte(), 0x05, "invalid byte value")
        assertEquals(expected, detailed, controlPacketSpectMatchError)
        val simple = PUBLISH.flags(qos = EXACTLY_ONCE, retain = true)
        assertEquals(expected, simple, controlPacketSpectMatchError)
    }

    @Test
    fun controlPacketFlagsMatchSpecForPUBLISH_dup_true_Qos_ExactlyOnce_retain_true() {
        val expected = FlagBits(bit3 = true, bit2 = true, bit0 = true)
        val simple = PUBLISH.flags(dup = true, qos = EXACTLY_ONCE, retain = true)
        assertEquals(simple.toByte(), 0x0D, "invalid byte value")
        assertEquals(expected, simple, controlPacketSpectMatchError)
    }

    @Test
    fun controlPacketFlagsMatchSpecForPUBACK() =
            assertEquals(emptyFlagBits, PUBACK.flags(), controlPacketSpectMatchError)

    @Test
    fun controlPacketFlagsMatchSpecForPUBREC() =
            assertEquals(emptyFlagBits, PUBREC.flags(), controlPacketSpectMatchError)

    @Test
    fun controlPacketFlagsMatchSpecForPUBREL() =
            assertEquals(bit1TrueFlagBits, PUBREL.flags(), controlPacketSpectMatchError)

    @Test
    fun controlPacketFlagsMatchSpecForPUBCOMP() =
            assertEquals(emptyFlagBits, PUBCOMP.flags(), controlPacketSpectMatchError)

    @Test
    fun controlPacketFlagsMatchSpecForSUBSCRIBE() =
            assertEquals(bit1TrueFlagBits, SUBSCRIBE.flags(), controlPacketSpectMatchError)

    @Test
    fun controlPacketFlagsMatchSpecForSUBACK() =
            assertEquals(emptyFlagBits, SUBACK.flags(), controlPacketSpectMatchError)

    @Test
    fun controlPacketFlagsMatchSpecForUNSUBSCRIBE() =
            assertEquals(bit1TrueFlagBits, UNSUBSCRIBE.flags(), controlPacketSpectMatchError)

    @Test
    fun controlPacketFlagsMatchSpecForUNSUBACK() =
            assertEquals(emptyFlagBits, UNSUBACK.flags(), controlPacketSpectMatchError)

    @Test
    fun controlPacketFlagsMatchSpecForPINGREQ() =
            assertEquals(emptyFlagBits, PINGREQ.flags(), controlPacketSpectMatchError)

    @Test
    fun controlPacketFlagsMatchSpecForPINGRESP() =
            assertEquals(emptyFlagBits, PINGRESP.flags(), controlPacketSpectMatchError)

    @Test
    fun controlPacketFlagsMatchSpecForDISCONNECT() =
            assertEquals(emptyFlagBits, DISCONNECT.flags(), controlPacketSpectMatchError)

    @Test
    fun controlPacketFlagsMatchSpecForAUTH() =
            assertEquals(emptyFlagBits, AUTH.flags(), controlPacketSpectMatchError)

    @Test
    fun emptyFlagBitsTest() {
        assertEquals(emptyFlagBits.toByte(), 0x00)
    }

    @Test
    fun bit1TrueFlagBitsTest() {
        assertEquals(bit1TrueFlagBits.toByte(), 0x02)
    }
}
