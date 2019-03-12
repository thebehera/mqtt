@file:Suppress("EXPERIMENTAL_API_USAGE")

package mqtt.wire.control.packet

import kotlinx.io.core.readBytes
import mqtt.wire.control.packet.format.ReasonCode.SUCCESS
import mqtt.wire.control.packet.format.ReasonCode.UNSPECIFIED_ERROR
import mqtt.wire.control.packet.format.fixed.get
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class ConnectionAcknowledgmentTests {
    @Test
    fun serializeDeserializeDefault() {
        val actual = ConnectionAcknowledgment()
        val bytes = actual.serialize().copy()
        val expected = ControlPacket.from(bytes)
        assertEquals(expected, actual)
    }

    @Test
    fun bit0SessionPresentFalseFlags() {
        val model = ConnectionAcknowledgment()
        val data = model.header.packet().readBytes()
        val sessionPresentBit = data[0].toUByte().get(0)
        assertFalse(sessionPresentBit)
        val result = ControlPacket.from(model.serialize()) as ConnectionAcknowledgment
        assertFalse(result.header.sessionPresent)
    }

    @Test
    fun bit0SessionPresentFlags() {
        val model = ConnectionAcknowledgment(ConnectionAcknowledgment.VariableHeader(true))
        val data = model.header.packet().readBytes()
        val sessionPresentBit = data[0].toUByte().get(0)
        assertTrue(sessionPresentBit)
    }

    @Test
    fun connectReasonCodeDefaultSuccess() {
        val model = ConnectionAcknowledgment()
        val headerData = model.header.packet().readBytes()
        val connectReasonByte = headerData[1].toUByte()
        assertEquals(connackConnectReason[connectReasonByte], SUCCESS)
        val allData = model.serialize()
        val result = ControlPacket.from(allData) as ConnectionAcknowledgment
        assertEquals(result.header.connectReason, SUCCESS)
    }

    @Test
    fun connectReasonCodeDefaultUnspecifiedError() {
        val model = ConnectionAcknowledgment(ConnectionAcknowledgment.VariableHeader(connectReason = UNSPECIFIED_ERROR))
        val data = model.header.packet().readBytes()
        val connectReasonByte = data[1].toUByte()
        assertEquals(connackConnectReason[connectReasonByte], UNSPECIFIED_ERROR)
    }
}