@file:Suppress("EXPERIMENTAL_API_USAGE")

package mqtt.wire.control.packet.format.fixed

import mqtt.wire.control.packet.format.fixed.ControlPacketType.*
import mqtt.wire.control.packet.format.fixed.DirectionOfFlow.*
import kotlin.test.Test
import kotlin.test.assertEquals

class TypeTests {

    val controlPacketSpectMatchError = "doesn't match the spec from " +
            "https://docs.oasis-open.org/mqtt/mqtt/v5.0/cos02/mqtt-v5.0-cos02.html#_Toc1477322"

    // Control packet types value matching spec
    @Test
    fun controlPacketTypeValueMatchesSpecForCONNECT() =
            assertEquals(1.toByte(), CONNECT.value, controlPacketSpectMatchError)

    @Test
    fun controlPacketTypeValueMatchesSpecForCONNACK() =
            assertEquals(2.toByte(), CONNACK.value, controlPacketSpectMatchError)

    @Test
    fun controlPacketTypeValueMatchesSpecForPUBLISH() =
            assertEquals(3.toByte(), PUBLISH.value, controlPacketSpectMatchError)

    @Test
    fun controlPacketTypeValueMatchesSpecForPUBACK() =
            assertEquals(4.toByte(), PUBACK.value, controlPacketSpectMatchError)

    @Test
    fun controlPacketTypeValueMatchesSpecForPUBREC() =
            assertEquals(5.toByte(), PUBREC.value, controlPacketSpectMatchError)

    @Test
    fun controlPacketTypeValueMatchesSpecForPUBREL() =
            assertEquals(6.toByte(), PUBREL.value, controlPacketSpectMatchError)

    @Test
    fun controlPacketTypeValueMatchesSpecForPUBCOMP() =
            assertEquals(7.toByte(), PUBCOMP.value, controlPacketSpectMatchError)

    @Test
    fun controlPacketTypeValueMatchesSpecForSUBSCRIBE() =
            assertEquals(8.toByte(), SUBSCRIBE.value, controlPacketSpectMatchError)

    @Test
    fun controlPacketTypeValueMatchesSpecForSUBACK() =
            assertEquals(9.toByte(), SUBACK.value, controlPacketSpectMatchError)

    @Test
    fun controlPacketTypeValueMatchesSpecForUNSUBSCRIBE() =
            assertEquals(10.toByte(), UNSUBSCRIBE.value, controlPacketSpectMatchError)

    @Test
    fun controlPacketTypeValueMatchesSpecForUNSUBACK() =
            assertEquals(11.toByte(), UNSUBACK.value, controlPacketSpectMatchError)

    @Test
    fun controlPacketTypeValueMatchesSpecForPINGREQ() =
            assertEquals(12.toByte(), PINGREQ.value, controlPacketSpectMatchError)

    @Test
    fun controlPacketTypeValueMatchesSpecForPINGRESP() =
            assertEquals(13.toByte(), PINGRESP.value, controlPacketSpectMatchError)

    @Test
    fun controlPacketTypeValueMatchesSpecForDISCONNECT() =
            assertEquals(14.toByte(), DISCONNECT.value, controlPacketSpectMatchError)

    @Test
    fun controlPacketTypeValueMatchesSpecForAUTH() =
            assertEquals(15.toByte(), AUTH.value, controlPacketSpectMatchError)


    // Control packet types direction of flow matching spec
    @Test
    fun controlPacketTypeDirectionOfFlowCONNECT() =
            assertEquals(CLIENT_TO_SERVER, CONNECT.direction, controlPacketSpectMatchError)

    @Test
    fun controlPacketTypeDirectionOfFlowCONNACK() =
            assertEquals(SERVER_TO_CLIENT, CONNACK.direction, controlPacketSpectMatchError)

    @Test
    fun controlPacketTypeDirectionOfFlowPUBLISH() =
            assertEquals(BIDIRECTIONAL, PUBLISH.direction, controlPacketSpectMatchError)

    @Test
    fun controlPacketTypeDirectionOfFlowPUBACK() =
            assertEquals(BIDIRECTIONAL, PUBACK.direction, controlPacketSpectMatchError)

    @Test
    fun controlPacketTypeDirectionOfFlowPUBREC() =
            assertEquals(BIDIRECTIONAL, PUBREC.direction, controlPacketSpectMatchError)

    @Test
    fun controlPacketTypeDirectionOfFlowPUBREL() =
            assertEquals(BIDIRECTIONAL, PUBREL.direction, controlPacketSpectMatchError)

    @Test
    fun controlPacketTypeDirectionOfFlowPUBCOMP() =
            assertEquals(BIDIRECTIONAL, PUBCOMP.direction, controlPacketSpectMatchError)

    @Test
    fun controlPacketTypeDirectionOfFlowSUBSCRIBE() =
            assertEquals(CLIENT_TO_SERVER, SUBSCRIBE.direction, controlPacketSpectMatchError)

    @Test
    fun controlPacketTypeDirectionOfFlowSUBACK() =
            assertEquals(SERVER_TO_CLIENT, SUBACK.direction, controlPacketSpectMatchError)

    @Test
    fun controlPacketTypeDirectionOfFlowUNSUBSCRIBE() =
            assertEquals(CLIENT_TO_SERVER, UNSUBSCRIBE.direction, controlPacketSpectMatchError)

    @Test
    fun controlPacketTypeDirectionOfFlowUNSUBACK() =
            assertEquals(SERVER_TO_CLIENT, UNSUBACK.direction, controlPacketSpectMatchError)

    @Test
    fun controlPacketTypeDirectionOfFlowPINGREQ() =
            assertEquals(CLIENT_TO_SERVER, PINGREQ.direction, controlPacketSpectMatchError)

    @Test
    fun controlPacketTypeDirectionOfFlowPINGRESP() =
            assertEquals(SERVER_TO_CLIENT, PINGRESP.direction, controlPacketSpectMatchError)

    @Test
    fun controlPacketTypeDirectionOfFlowDISCONNECT() =
            assertEquals(BIDIRECTIONAL, DISCONNECT.direction, controlPacketSpectMatchError)

    @Test
    fun controlPacketTypeDirectionOfFlowAUTH() =
            assertEquals(BIDIRECTIONAL, AUTH.direction, controlPacketSpectMatchError)
}
