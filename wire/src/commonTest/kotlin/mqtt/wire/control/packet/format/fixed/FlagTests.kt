@file:Suppress("EXPERIMENTAL_API_USAGE")

package mqtt.wire.control.packet.format.fixed

import kotlinx.io.core.readBytes
import mqtt.wire.control.packet.*
import mqtt.wire.control.packet.PublishMessage.FixedHeader
import mqtt.wire.control.packet.PublishMessage.VariableHeader
import mqtt.wire.data.MqttUtf8String
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
            assertEquals(ConnectionAcknowledgment().flags, 0b0, controlPacketSpectMatchError)

    @Test
    fun controlPacketFlagsMatchSpecForPUBLISH_dup_false_Qos_AtMostOnce_Retain_false() {
        val variable = VariableHeader(MqttUtf8String("t"))
        val detailed = PublishMessage(variable = variable)
        assertEquals(detailed.controlPacketValue, 0x03, "invalid byte controlPacketValue")
        assertEquals(detailed.flags, 0b0, controlPacketSpectMatchError)
    }


    @Test
    fun controlPacketFlagsMatchSpecForPUBLISH_dup_trueQos_AtMostOnceRetain_false() {
        val expected = 0b1000.toByte()
        val fixed = FixedHeader(dup = true, qos = AT_MOST_ONCE, retain = false)
        val variable = VariableHeader(MqttUtf8String("t"))
        val detailed = PublishMessage(fixed = fixed, variable = variable)
        assertEquals(detailed.controlPacketValue, 0x03, "invalid byte controlPacketValue")
        assertEquals(expected, detailed.flags, controlPacketSpectMatchError)
        val fixed1 = FixedHeader(dup = true)
        val simple = PublishMessage(fixed = fixed1, variable = variable)
        assertEquals(expected, simple.flags, controlPacketSpectMatchError)
    }

    @Test
    fun controlPacketFlagsMatchSpecForPUBLISH_dup_false_Qos_AtMostOnce_retain_true() {
        val expected = 0b1.toByte()
        val fixed = FixedHeader(dup = false, qos = AT_MOST_ONCE, retain = true)
        val variable = VariableHeader(MqttUtf8String("t"))
        val detailed = PublishMessage(fixed = fixed, variable = variable)
        assertEquals(detailed.controlPacketValue, 0x03, "invalid byte controlPacketValue")
        assertEquals(expected, detailed.flags, controlPacketSpectMatchError)
        val fixed1 = FixedHeader(retain = true)
        val simple = PublishMessage(fixed = fixed1, variable = variable)
        assertEquals(expected, simple.flags, controlPacketSpectMatchError)
    }

    @Test
    fun controlPacketFlagsMatchSpecForPUBLISH_dup_true_Qos_AtMostOnce_retain_true() {
        val expected = 0b1001.toByte()
        val fixed = FixedHeader(dup = true, qos = AT_MOST_ONCE, retain = true)
        val variable = VariableHeader(MqttUtf8String("t"))
        val detailed = PublishMessage(fixed, variable)
        assertEquals(detailed.controlPacketValue, 0x03, "invalid byte controlPacketValue")
        assertEquals(expected, detailed.flags, controlPacketSpectMatchError)
    }

    @Test // THIS IS WHAT I NEED TO WORK ON FIRST FIX THIS
    fun controlPacketFlagsMatchSpecForPUBLISH_dup_false_Qos_AtLeastOnce_retain_false() {
        val expected = 0b10.toByte()
        val fixed = FixedHeader(dup = false, qos = AT_LEAST_ONCE, retain = false)
        val variable = VariableHeader(MqttUtf8String("t"), packetIdentifier = packetIdentifier)
        val detailed = PublishMessage(fixed, variable)
        assertEquals(detailed.controlPacketValue, 0x03,
                "Invalid Byte 1 in the fixed header: Control Packet Value")
        val byteAsUInt = detailed.serialize().readBytes()[0].toUInt()

        assertEquals(byteAsUInt.shr(4), 0x03.toUInt(),
                "Invalid Byte 1 in the fixed header: Control Packet Value serialize shift right 4 times")
        val expectedFlagMatch = byteAsUInt.shl(4).toByte().toInt().shr(4).toByte()
        assertEquals(expectedFlagMatch, 0b0010,
                "Invalid Byte 1 in the fixed header: Flags dont match")
        assertEquals(expected, detailed.flags, controlPacketSpectMatchError)
    }

    @Test
    fun controlPacketFlagsMatchSpecForPUBLISH_dup_true_Qos_AtLeastOnce_retain_false() {
        val expected = 0b1010.toByte()
        val fixed = FixedHeader(dup = true, qos = AT_LEAST_ONCE, retain = false)
        val variable = VariableHeader(MqttUtf8String("t"), packetIdentifier = packetIdentifier)
        val detailed = PublishMessage(fixed, variable)
        assertEquals(detailed.controlPacketValue, 0x03, "invalid byte controlPacketValue")
        assertEquals(expected, detailed.flags, controlPacketSpectMatchError)
    }

    @Test
    fun controlPacketFlagsMatchSpecForPUBLISH_dup_false_Qos_AtLeastOnce_retain_true() {
        val expected = 0b11.toByte()
        val fixed = FixedHeader(dup = false, qos = AT_LEAST_ONCE, retain = true)
        val variable = VariableHeader(MqttUtf8String("t"), packetIdentifier = packetIdentifier)
        val detailed = PublishMessage(fixed, variable)
        assertEquals(detailed.controlPacketValue, 0x03, "invalid byte controlPacketValue")
        assertEquals(expected, detailed.flags, controlPacketSpectMatchError)
    }

    @Test
    fun controlPacketFlagsMatchSpecForPUBLISH_dup_true_Qos_AtLeastOnce_retain_true() {
        val expected = 0b1011.toByte()
        val fixed = FixedHeader(dup = true, qos = AT_LEAST_ONCE, retain = true)
        val variable = VariableHeader(MqttUtf8String("t"), packetIdentifier = packetIdentifier)
        val simple = PublishMessage(fixed, variable)
        assertEquals(simple.controlPacketValue, 0x03, "invalid byte controlPacketValue")
        assertEquals(expected, simple.flags, controlPacketSpectMatchError)
    }

    @Test
    fun controlPacketFlagsMatchSpecForPUBLISH_dup_false_Qos_ExactlyOnce_retain_false() {
        val expected = 0b100.toByte()
        val fixed = FixedHeader(dup = false, qos = EXACTLY_ONCE, retain = false)
        val variable = VariableHeader(MqttUtf8String("t"), packetIdentifier = packetIdentifier)
        val detailed = PublishMessage(fixed, variable)
        assertEquals(detailed.controlPacketValue, 0x03, "invalid byte controlPacketValue")
        assertEquals(expected, detailed.flags, controlPacketSpectMatchError)
    }

    @Test
    fun controlPacketFlagsMatchSpecForPUBLISH_dup_true_Qos_ExactlyOnce_retain_false() {
        val expected = 0b1100.toByte()
        val fixed = FixedHeader(dup = true, qos = EXACTLY_ONCE, retain = false)
        val variable = VariableHeader(MqttUtf8String("t"), packetIdentifier = packetIdentifier)
        val detailed = PublishMessage(fixed, variable)
        assertEquals(detailed.controlPacketValue, 0x03, "invalid byte controlPacketValue")
        assertEquals(expected, detailed.flags, controlPacketSpectMatchError)
    }

    @Test
    fun controlPacketFlagsMatchSpecForPUBLISH_dup_false_Qos_ExactlyOnce_retain_true() {
        val expected = 0b101.toByte()
        val fixed = FixedHeader(dup = false, qos = EXACTLY_ONCE, retain = true)
        val variable = VariableHeader(MqttUtf8String("t"), packetIdentifier = packetIdentifier)
        val detailed = PublishMessage(fixed, variable)
        assertEquals(detailed.controlPacketValue, 0x03, "invalid byte controlPacketValue")
        assertEquals(expected, detailed.flags, controlPacketSpectMatchError)
    }

    @Test
    fun controlPacketFlagsMatchSpecForPUBLISH_dup_true_Qos_ExactlyOnce_retain_true() {
        val expected = 0b1101.toByte()
        val fixed = FixedHeader(dup = true, qos = EXACTLY_ONCE, retain = true)
        val variable = VariableHeader(MqttUtf8String("t"), packetIdentifier = packetIdentifier)
        val simple = PublishMessage(fixed, variable)
        assertEquals(simple.controlPacketValue, 0x03, "invalid byte controlPacketValue")
        assertEquals(expected, simple.flags, controlPacketSpectMatchError)
    }

    @Test
    fun controlPacketFlagsMatchSpecForPUBACK() =
            assertEquals(PublishAcknowledgment(PublishAcknowledgment.VariableHeader(packetIdentifier)).flags, 0b0, controlPacketSpectMatchError)

    @Test
    fun controlPacketFlagsMatchSpecForPUBREC() =
            assertEquals(PublishReceived(PublishReceived.VariableHeader(packetIdentifier)).flags, 0b0, controlPacketSpectMatchError)

    @Test
    fun controlPacketFlagsMatchSpecForPUBREL() =
            assertEquals(PublishRelease(PublishRelease.VariableHeader(packetIdentifier)).flags, 0b10, controlPacketSpectMatchError)

    @Test
    fun controlPacketFlagsMatchSpecForPUBCOMP() =
            assertEquals(PublishComplete(PublishComplete.VariableHeader(packetIdentifier)).flags, 0b0, controlPacketSpectMatchError)

    @Test
    fun controlPacketFlagsMatchSpecForSUBSCRIBE() =
            assertEquals(0b10, SubscribeRequest(SubscribeRequest.VariableHeader(packetIdentifier), setOf(Subscription(MqttUtf8String("yolo")))).flags, controlPacketSpectMatchError)

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

    @Test
    fun uByteToBoolean0b0() = assertEquals(0b00000000.toUByte().get(0), false)

    @Test
    fun uByteToBoolean0b00000001First() = assertEquals(0b00000001.toUByte().get(0), true)


    @Test
    fun uByteToBoolean0b00000001Last() {
        val ubyte = 0b10000000.toUByte()
        val ubyteAsInt = ubyte.toInt()
        val intShifted = ubyteAsInt.shl(7 - 7)
        val shiftedAsUbyte = intShifted.toUByte()
        val shiftedAsInt = shiftedAsUbyte.toInt()
        val shiftedRight = shiftedAsInt.shr(7)

        assertEquals(shiftedRight == 1, true)
    }


}
