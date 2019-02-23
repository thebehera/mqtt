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
    fun `Control Packet Flags match spec for CONNECT`() {
        assertEquals(emptyFlagBits, CONNECT.flags(), controlPacketSpectMatchError)
    }

    @Test
    fun `Control Packet Flags match spec for CONNACK`() =
            assertEquals(emptyFlagBits, CONNACK.flags(), controlPacketSpectMatchError)

    @Test
    fun `Control Packet Flags match spec for PUBLISH dup=false qos=AtMostOnce retain=false`() {
        val detailed = PUBLISH.flags(dup = false, qos = AT_MOST_ONCE, retain = false)
        assertEquals(emptyFlagBits, detailed, controlPacketSpectMatchError)
        val simple = PUBLISH.flags()
        assertEquals(emptyFlagBits, simple, controlPacketSpectMatchError)
    }

    @Test
    fun `Control Packet Flags match spec for PUBLISH dup=true qos=AtMostOnce retain=false`() {
        val expected = FlagBits(bit3 = true)
        val detailed = PUBLISH.flags(dup = true, qos = AT_MOST_ONCE, retain = false)
        assertEquals(expected, detailed, controlPacketSpectMatchError)
        val simple = PUBLISH.flags(dup = true)
        assertEquals(expected, simple, controlPacketSpectMatchError)
    }

    @Test
    fun `Control Packet Flags match spec for PUBLISH dup=false qos=AtMostOnce retain=true`() {
        val expected = FlagBits(bit0 = true)
        val detailed = PUBLISH.flags(dup = false, qos = AT_MOST_ONCE, retain = true)
        assertEquals(expected, detailed, controlPacketSpectMatchError)
        val simple = PUBLISH.flags(retain = true)
        assertEquals(expected, simple, controlPacketSpectMatchError)
    }

    @Test
    fun `Control Packet Flags match spec for PUBLISH dup=true qos=AtMostOnce retain=true`() {
        val expected = FlagBits(bit3 = true, bit0 = true)
        val detailed = PUBLISH.flags(dup = true, qos = AT_MOST_ONCE, retain = true)
        assertEquals(expected, detailed, controlPacketSpectMatchError)
        val simple = PUBLISH.flags(dup = true, retain = true)
        assertEquals(expected, simple, controlPacketSpectMatchError)
    }

    @Test
    fun `Control Packet Flags match spec for PUBLISH dup=false qos=AtLeastOnce retain=false`() {
        val expected = bit1TrueFlagBits
        val detailed = PUBLISH.flags(dup = false, qos = AT_LEAST_ONCE, retain = false)
        assertEquals(expected, detailed, controlPacketSpectMatchError)
        val simple = PUBLISH.flags(qos = AT_LEAST_ONCE)
        assertEquals(expected, simple, controlPacketSpectMatchError)
    }

    @Test
    fun `Control Packet Flags match spec for PUBLISH dup=true qos=AtLeastOnce retain=false`() {
        val expected = FlagBits(bit3 = true, bit1 = true)
        val detailed = PUBLISH.flags(dup = true, qos = AT_LEAST_ONCE, retain = false)
        assertEquals(expected, detailed, controlPacketSpectMatchError)
        val simple = PUBLISH.flags(dup = true, qos = AT_LEAST_ONCE)
        assertEquals(expected, simple, controlPacketSpectMatchError)
    }

    @Test
    fun `Control Packet Flags match spec for PUBLISH dup=false qos=AtLeastOnce retain=true`() {
        val expected = FlagBits(bit1 = true, bit0 = true)
        val detailed = PUBLISH.flags(dup = false, qos = AT_LEAST_ONCE, retain = true)
        assertEquals(expected, detailed, controlPacketSpectMatchError)
        val simple = PUBLISH.flags(qos = AT_LEAST_ONCE, retain = true)
        assertEquals(expected, simple, controlPacketSpectMatchError)
    }

    @Test
    fun `Control Packet Flags match spec for PUBLISH dup=true qos=AtLeastOnce retain=true`() {
        val expected = FlagBits(bit3 = true, bit1 = true, bit0 = true)
        val simple = PUBLISH.flags(dup = true, qos = AT_LEAST_ONCE, retain = true)
        assertEquals(expected, simple, controlPacketSpectMatchError)
    }

    @Test
    fun `Control Packet Flags match spec for PUBLISH dup=false qos=ExactlyOnce retain=false`() {
        val expected = FlagBits(bit2 = true)
        val detailed = PUBLISH.flags(dup = false, qos = EXACTLY_ONCE, retain = false)
        assertEquals(expected, detailed, controlPacketSpectMatchError)
        val simple = PUBLISH.flags(qos = EXACTLY_ONCE)
        assertEquals(expected, simple, controlPacketSpectMatchError)
    }

    @Test
    fun `Control Packet Flags match spec for PUBLISH dup=true qos=ExactlyOnce retain=false`() {
        val expected = FlagBits(bit3 = true, bit2 = true)
        val detailed = PUBLISH.flags(dup = true, qos = EXACTLY_ONCE, retain = false)
        assertEquals(expected, detailed, controlPacketSpectMatchError)
        val simple = PUBLISH.flags(dup = true, qos = EXACTLY_ONCE)
        assertEquals(expected, simple, controlPacketSpectMatchError)
    }

    @Test
    fun `Control Packet Flags match spec for PUBLISH dup=false qos=ExactlyOnce retain=true`() {
        val expected = FlagBits(bit2 = true, bit0 = true)
        val detailed = PUBLISH.flags(dup = false, qos = EXACTLY_ONCE, retain = true)
        assertEquals(expected, detailed, controlPacketSpectMatchError)
        val simple = PUBLISH.flags(qos = EXACTLY_ONCE, retain = true)
        assertEquals(expected, simple, controlPacketSpectMatchError)
    }

    @Test
    fun `Control Packet Flags match spec for PUBLISH dup=true qos=ExactlyOnce retain=true`() {
        val expected = FlagBits(bit3 = true, bit2 = true, bit0 = true)
        val simple = PUBLISH.flags(dup = true, qos = EXACTLY_ONCE, retain = true)
        assertEquals(expected, simple, controlPacketSpectMatchError)
    }

    @Test
    fun `Control Packet Flags match spec for PUBACK`() =
            assertEquals(emptyFlagBits, PUBACK.flags(), controlPacketSpectMatchError)

    @Test
    fun `Control Packet Flags match spec for PUBREC`() =
            assertEquals(emptyFlagBits, PUBREC.flags(), controlPacketSpectMatchError)

    @Test
    fun `Control Packet Flags match spec for PUBREL`() =
            assertEquals(bit1TrueFlagBits, PUBREL.flags(), controlPacketSpectMatchError)

    @Test
    fun `Control Packet Flags match spec for PUBCOMP`() =
            assertEquals(emptyFlagBits, PUBCOMP.flags(), controlPacketSpectMatchError)

    @Test
    fun `Control Packet Flags match spec for SUBSCRIBE`() =
            assertEquals(bit1TrueFlagBits, SUBSCRIBE.flags(), controlPacketSpectMatchError)

    @Test
    fun `Control Packet Flags match spec for SUBACK`() =
            assertEquals(emptyFlagBits, SUBACK.flags(), controlPacketSpectMatchError)

    @Test
    fun `Control Packet Flags match spec for UNSUBSCRIBE`() =
            assertEquals(bit1TrueFlagBits, UNSUBSCRIBE.flags(), controlPacketSpectMatchError)

    @Test
    fun `Control Packet Flags match spec for UNSUBACK`() =
            assertEquals(emptyFlagBits, UNSUBACK.flags(), controlPacketSpectMatchError)

    @Test
    fun `Control Packet Flags match spec for PINGREQ`() =
            assertEquals(emptyFlagBits, PINGREQ.flags(), controlPacketSpectMatchError)

    @Test
    fun `Control Packet Flags match spec for PINGRESP`() =
            assertEquals(emptyFlagBits, PINGRESP.flags(), controlPacketSpectMatchError)

    @Test
    fun `Control Packet Flags match spec for DISCONNECT`() =
            assertEquals(emptyFlagBits, DISCONNECT.flags(), controlPacketSpectMatchError)

    @Test
    fun `Control Packet Flags match spec for AUTH`() =
            assertEquals(emptyFlagBits, AUTH.flags(), controlPacketSpectMatchError)

}
