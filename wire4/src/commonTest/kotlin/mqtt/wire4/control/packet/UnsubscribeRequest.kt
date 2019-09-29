@file:Suppress("EXPERIMENTAL_API_USAGE")

package mqtt.wire4.control.packet

import mqtt.wire.data.MqttUtf8String
import kotlin.test.Test
import kotlin.test.assertEquals

class UnsubscribeRequestTests {

    private val packetIdentifier = 2

    @Test
    fun basicTest() {
        val unsub = UnsubscribeRequest(packetIdentifier, listOf(MqttUtf8String("yolo"), MqttUtf8String("yolo")))
        val result = ControlPacketV4.from(unsub.serialize()) as UnsubscribeRequest
        assertEquals(result.topics.first().getValueOrThrow(), "yolo")
    }
}
