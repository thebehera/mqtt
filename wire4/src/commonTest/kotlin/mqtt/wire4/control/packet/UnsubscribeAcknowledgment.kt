@file:Suppress("EXPERIMENTAL_API_USAGE")

package mqtt.wire4.control.packet

import mqtt.buffer.allocateNewBuffer
import kotlin.test.Test
import kotlin.test.assertEquals

class UnsubscribeAcknowledgmentTests {
    private val packetIdentifier = 2
    @Test
    fun serializeDeserializeDefault() {
        val buffer = allocateNewBuffer(4u, limits)
        val actual = UnsubscribeAcknowledgment(packetIdentifier)
        actual.serialize(buffer)
        buffer.resetForRead()
        val expected = ControlPacketV4.from(buffer)
        assertEquals(expected, actual)
    }
}
