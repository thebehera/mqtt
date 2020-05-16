@file:Suppress("EXPERIMENTAL_API_USAGE", "EXPERIMENTAL_UNSIGNED_LITERALS")

package mqtt.wire5.control.packet

import mqtt.buffer.allocateNewBuffer
import mqtt.wire5.control.packet.UnsubscribeRequest.VariableHeader
import mqtt.wire5.control.packet.format.variable.property.UserProperty
import kotlin.test.Test
import kotlin.test.assertEquals

class UnsubscribeRequestTests {

    private val packetIdentifier = 2

    @Test
    fun basicTest() {
        val buffer = allocateNewBuffer(11u, limits)
        val unsub = UnsubscribeRequest(VariableHeader(packetIdentifier), setOf("yolo".toCharSequenceBuffer()))
        unsub.serialize(buffer)
        buffer.resetForRead()
        assertEquals(0b10100010.toByte(), buffer.readByte(), "fixed header byte 1")
        assertEquals(9u, buffer.readVariableByteInteger(), "fixed header byte 2 remaining length")
        assertEquals(
            packetIdentifier.toUShort(),
            buffer.readUnsignedShort(),
            "variable header byte 1-2 packet identifier"
        )
        assertEquals(0u, buffer.readVariableByteInteger(), "variable header byte 3 property length")
        assertEquals("yolo", buffer.readMqttUtf8StringNotValidated().toString(), "payload topic")
        buffer.resetForRead()
        val result = ControlPacketV5.from(buffer) as UnsubscribeRequest
        assertEquals("yolo", result.topics.first().toString())
        assertEquals(unsub, result)
    }

    @Test
    fun variableHeaderPropertyUserProperty() {
        val props = VariableHeader.Properties.from(setOf(UserProperty("key", "value")))
        val userPropertyResult = props.userProperty
        for ((key, value) in userPropertyResult) {
            assertEquals(key, "key")
            assertEquals(value, "value")
        }
        assertEquals(userPropertyResult.size, 1)

        val request =
            UnsubscribeRequest(VariableHeader(packetIdentifier, properties = props), setOf("test"))
        val buffer = allocateNewBuffer(24u, limits)
        request.serialize(buffer)
        buffer.resetForRead()
        val requestRead = ControlPacketV5.from(buffer) as UnsubscribeRequest
        val (key, value) = requestRead.variable.properties.userProperty.first()
        assertEquals("key", key.toString())
        assertEquals("value", value.toString())
    }
}
