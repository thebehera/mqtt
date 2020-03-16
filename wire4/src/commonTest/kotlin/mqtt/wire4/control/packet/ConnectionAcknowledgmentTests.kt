@file:Suppress("EXPERIMENTAL_API_USAGE")

package mqtt.wire4.control.packet

import mqtt.buffer.allocateNewBuffer
import mqtt.wire.control.packet.format.fixed.get
import mqtt.wire4.control.packet.ConnectionAcknowledgment.VariableHeader
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue


class ConnectionAcknowledgmentTests {

    @Test
    fun serializeDeserializeDefault() {
        val buffer = allocateNewBuffer(4u, limits)
        val actual = ConnectionAcknowledgment()
        actual.serialize(buffer)
        buffer.resetForRead()
        val expected = ControlPacketV4.from(buffer)
        assertEquals(expected, actual)
    }

    @Test
    fun bit0SessionPresentFalseFlags() {
        val buffer = allocateNewBuffer(4u, limits)
        val model = ConnectionAcknowledgment()
        model.header.serialize(buffer)
        buffer.resetForRead()
        val sessionPresentBit = buffer.readUnsignedByte().get(0)
        assertFalse(sessionPresentBit)

        val buffer2 = allocateNewBuffer(4u, limits)
        model.serialize(buffer2)
        buffer2.resetForRead()
        val result = ControlPacketV4.from(buffer2) as ConnectionAcknowledgment
        assertFalse(result.header.sessionPresent)
    }

    @Test
    fun bit0SessionPresentFlags() {
        val buffer = allocateNewBuffer(4u, limits)
        val model = ConnectionAcknowledgment(VariableHeader(true))
        model.header.serialize(buffer)
        buffer.resetForRead()
        assertTrue(buffer.readUnsignedByte().get(0))
    }
}

