@file:Suppress("EXPERIMENTAL_API_USAGE")

package mqtt.wire4.control.packet

import mqtt.wire.data.MqttUtf8String
import kotlin.test.Test
import kotlin.test.assertEquals

class SubscribeRequestTest {

    @Test
    fun serializeDeserialize() {
        val subscribeRequest = SubscribeRequest(2.toUShort(), listOf(Subscription(MqttUtf8String("test"))))
        assertEquals(subscribeRequest.packetIdentifier, 2.toUShort())
        assertEquals(subscribeRequest.subscriptions.first().topicFilter.getValueOrThrow(), "test")
        val subscribeRequestData = subscribeRequest.serialize()
        val requestRead = ControlPacket.from(subscribeRequestData) as SubscribeRequest
        assertEquals(requestRead.subscriptions.first().topicFilter.getValueOrThrow(), "test")
    }
}
