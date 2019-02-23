package mqtt.wire.control.packet.fixed

import mqtt.wire.control.packet.fixed.ControlPacketType.*
import mqtt.wire.control.packet.fixed.DirectionOfFlow.*
import kotlin.test.Test
import kotlin.test.assertEquals

class TypeTests {

    val controlPacketSpectMatchError = "doesn't match the spec from " +
            "https://docs.oasis-open.org/mqtt/mqtt/v5.0/cos02/mqtt-v5.0-cos02.html#_Toc1477322"

    // Control packet types value matching spec
    @Test
    fun `Control Packet Type value matches spec for CONNECT`() =
            assertEquals(1, CONNECT.value, controlPacketSpectMatchError)

    @Test
    fun `Control Packet Type value matches spec for CONNACK`() =
            assertEquals(2, CONNACK.value, controlPacketSpectMatchError)

    @Test
    fun `Control Packet Type value matches spec for PUBLISH`() =
            assertEquals(3, PUBLISH.value, controlPacketSpectMatchError)

    @Test
    fun `Control Packet Type value matches spec for PUBACK`() =
            assertEquals(4, PUBACK.value, controlPacketSpectMatchError)

    @Test
    fun `Control Packet Type value matches spec for PUBREC`() =
            assertEquals(5, PUBREC.value, controlPacketSpectMatchError)

    @Test
    fun `Control Packet Type value matches spec for PUBREL`() =
            assertEquals(6, PUBREL.value, controlPacketSpectMatchError)

    @Test
    fun `Control Packet Type value matches spec for PUBCOMP`() =
            assertEquals(7, PUBCOMP.value, controlPacketSpectMatchError)

    @Test
    fun `Control Packet Type value matches spec for SUBSCRIBE`() =
            assertEquals(8, SUBSCRIBE.value, controlPacketSpectMatchError)

    @Test
    fun `Control Packet Type value matches spec for SUBACK`() =
            assertEquals(9, SUBACK.value, controlPacketSpectMatchError)

    @Test
    fun `Control Packet Type value matches spec for UNSUBSCRIBE`() =
            assertEquals(10, UNSUBSCRIBE.value, controlPacketSpectMatchError)

    @Test
    fun `Control Packet Type value matches spec for UNSUBACK`() =
            assertEquals(11, UNSUBACK.value, controlPacketSpectMatchError)

    @Test
    fun `Control Packet Type value matches spec for PINGREQ`() =
            assertEquals(12, PINGREQ.value, controlPacketSpectMatchError)

    @Test
    fun `Control Packet Type value matches spec for PINGRESP`() =
            assertEquals(13, PINGRESP.value, controlPacketSpectMatchError)

    @Test
    fun `Control Packet Type value matches spec for DISCONNECT`() =
            assertEquals(14, DISCONNECT.value, controlPacketSpectMatchError)

    @Test
    fun `Control Packet Type value matches spec for AUTH`() =
            assertEquals(15, AUTH.value, controlPacketSpectMatchError)


    // Control packet types direction of flow matching spec
    @Test
    fun `Control Packet Type direction of flow CONNECT`() =
            assertEquals(CLIENT_TO_SERVER, CONNECT.direction, controlPacketSpectMatchError)

    @Test
    fun `Control Packet Type direction of flow CONNACK`() =
            assertEquals(SERVER_TO_CLIENT, CONNACK.direction, controlPacketSpectMatchError)

    @Test
    fun `Control Packet Type direction of flow PUBLISH`() =
            assertEquals(BIDIRECTIONAL, PUBLISH.direction, controlPacketSpectMatchError)

    @Test
    fun `Control Packet Type direction of flow PUBACK`() =
            assertEquals(BIDIRECTIONAL, PUBACK.direction, controlPacketSpectMatchError)

    @Test
    fun `Control Packet Type direction of flow PUBREC`() =
            assertEquals(BIDIRECTIONAL, PUBREC.direction, controlPacketSpectMatchError)

    @Test
    fun `Control Packet Type direction of flow PUBREL`() =
            assertEquals(BIDIRECTIONAL, PUBREL.direction, controlPacketSpectMatchError)

    @Test
    fun `Control Packet Type direction of flow PUBCOMP`() =
            assertEquals(BIDIRECTIONAL, PUBCOMP.direction, controlPacketSpectMatchError)

    @Test
    fun `Control Packet Type direction of flow SUBSCRIBE`() =
            assertEquals(CLIENT_TO_SERVER, SUBSCRIBE.direction, controlPacketSpectMatchError)

    @Test
    fun `Control Packet Type direction of flow SUBACK`() =
            assertEquals(SERVER_TO_CLIENT, SUBACK.direction, controlPacketSpectMatchError)

    @Test
    fun `Control Packet Type direction of flow UNSUBSCRIBE`() =
            assertEquals(CLIENT_TO_SERVER, UNSUBSCRIBE.direction, controlPacketSpectMatchError)

    @Test
    fun `Control Packet Type direction of flow UNSUBACK`() =
            assertEquals(SERVER_TO_CLIENT, UNSUBACK.direction, controlPacketSpectMatchError)

    @Test
    fun `Control Packet Type direction of flow PINGREQ`() =
            assertEquals(CLIENT_TO_SERVER, PINGREQ.direction, controlPacketSpectMatchError)

    @Test
    fun `Control Packet Type direction of flow PINGRESP`() =
            assertEquals(SERVER_TO_CLIENT, PINGRESP.direction, controlPacketSpectMatchError)

    @Test
    fun `Control Packet Type direction of flow DISCONNECT`() =
            assertEquals(BIDIRECTIONAL, DISCONNECT.direction, controlPacketSpectMatchError)

    @Test
    fun `Control Packet Type direction of flow AUTH`() =
            assertEquals(BIDIRECTIONAL, AUTH.direction, controlPacketSpectMatchError)
}
