@file:Suppress("EXPERIMENTAL_API_USAGE")

package mqtt.wire4.control.packet

import kotlinx.io.core.readBytes
import mqtt.wire.control.packet.format.fixed.get
import mqtt.wire4.control.packet.ConnectionAcknowledgment.VariableHeader
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class ConnectionAcknowledgmentTests {
    @Test
    fun serializeDeserializeDefault() {
        val actual = ConnectionAcknowledgment()
        val bytes = actual.serialize()
        val expected = ControlPacketV4.from(bytes)
        assertEquals(expected, actual)
    }

    @Test
    fun bit0SessionPresentFalseFlags() {
        val model = ConnectionAcknowledgment()
        val data = model.header.packet().readBytes()
        val sessionPresentBit = data[0].toUByte().get(0)
        assertFalse(sessionPresentBit)
        val result = ControlPacketV4.from(model.serialize()) as ConnectionAcknowledgment
        assertFalse(result.header.sessionPresent)
    }

    @Test
    fun bit0SessionPresentFlags() {
        val model = ConnectionAcknowledgment(VariableHeader(true))
        val data = model.header.packet().readBytes()
        val sessionPresentBit = data[0].toUByte().get(0)
        assertTrue(sessionPresentBit)
    }
}
