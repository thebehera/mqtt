@file:Suppress("EXPERIMENTAL_API_USAGE", "EXPERIMENTAL_UNSIGNED_LITERALS")

package mqtt.wire4.control.packet

import mqtt.buffer.allocateNewBuffer
import mqtt.wire.data.MqttUtf8String
import kotlin.test.Test
import kotlin.test.assertEquals

class UnsubscribeRequestTests {
    private val packetIdentifier = 2

    @Test
    fun basicTest() {
        val buffer = allocateNewBuffer(17u, limits)
        val unsub = UnsubscribeRequest(packetIdentifier, listOf(MqttUtf8String("yolo"), MqttUtf8String("yolo1")))
        unsub.serialize(buffer)
        buffer.resetForRead()
        val result = ControlPacketV4.from(buffer) as UnsubscribeRequest
        assertEquals(result.topics.first().getValueOrThrow().toString(), "yolo")
        assertEquals(result.topics[1].getValueOrThrow().toString(), "yolo1")
    }
}
