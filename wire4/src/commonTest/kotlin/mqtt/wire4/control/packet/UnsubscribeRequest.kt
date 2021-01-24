@file:Suppress("EXPERIMENTAL_API_USAGE", "EXPERIMENTAL_UNSIGNED_LITERALS")

package mqtt.wire4.control.packet

import mqtt.buffer.allocateNewBuffer
import kotlin.test.Test
import kotlin.test.assertEquals

class UnsubscribeRequestTests {
    private val packetIdentifier = 2

    @Test
    fun basicTest() {
        val buffer = allocateNewBuffer(17u)
        val unsub = UnsubscribeRequest(packetIdentifier, listOf(("yolo"), ("yolo1")))
        unsub.serialize(buffer)
        buffer.resetForRead()
        val result = ControlPacketV4.from(buffer) as UnsubscribeRequest
        assertEquals(result.topics.first().toString(), "yolo")
        assertEquals(result.topics[1].toString(), "yolo1")
    }
}
