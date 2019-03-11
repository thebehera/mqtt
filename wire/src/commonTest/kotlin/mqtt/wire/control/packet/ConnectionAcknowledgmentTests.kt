package mqtt.wire.control.packet

import kotlin.test.Test
import kotlin.test.assertEquals

class ConnectionAcknowledgmentTests {
    @Test
    fun serializeDeserializeDefault() {
        val actual = ConnectionAcknowledgment()
        val bytes = actual.serialize().copy()
        val expected = ControlPacket.from(bytes)
        assertEquals(expected, actual)
    }
}