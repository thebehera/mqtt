@file:Suppress("EXPERIMENTAL_API_USAGE")

package mqtt.wire4.control.packet

import kotlinx.io.core.readUByte
import kotlinx.io.core.readUShort
import mqtt.wire.data.QualityOfService.AT_LEAST_ONCE
import mqtt.wire.data.QualityOfService.EXACTLY_ONCE
import mqtt.wire.data.topic.Filter
import kotlin.test.Test
import kotlin.test.assertEquals

class SubscribeRequestTest {

    @Test
    fun serializeTestByteArray() {
        val subscription = Subscription.from(listOf("a/b", "c/d"), listOf(AT_LEAST_ONCE, EXACTLY_ONCE))
        val readPacket = Subscription.writeMany(subscription)
        assertEquals(12, readPacket.remaining)
        // Topic Filter ("a/b")
        // byte 1: Length MSB (0)
        assertEquals(0b00000000, readPacket.readByte())
        // byte2: Length LSB (3)
        assertEquals(0b00000011, readPacket.readByte())
        // byte3: a (0x61)
        assertEquals(0b01100001, readPacket.readByte())
        // byte4: / (0x2F)
        assertEquals(0b00101111, readPacket.readByte())
        // byte5: b (0x62)
        assertEquals(0b01100010, readPacket.readByte())
        // Subscription Options
        // byte6: Subscription Options (1)
        assertEquals(0b00000001, readPacket.readByte())


        // Topic Filter ("c/d")
        // byte 1: Length MSB (0)
        assertEquals(0b00000000, readPacket.readByte())
        // byte2: Length LSB (3)
        assertEquals(0b00000011, readPacket.readByte())
        // byte3: c (0x63)
        assertEquals(0b01100011, readPacket.readByte())
        // byte4: / (0x2F)
        assertEquals(0b00101111, readPacket.readByte())
        // byte5: d (0x64)
        assertEquals(0b01100100, readPacket.readByte())
        // Subscription Options
        // byte6: Subscription Options (2)
        assertEquals(0b00000010, readPacket.readByte())
        // No more bytes to read
        assertEquals(0, readPacket.remaining)
    }

    @Test
    fun subscriptionPayloadSize() {
        val subscriptions = Subscription.from(listOf("a/b", "c/d"), listOf(AT_LEAST_ONCE, EXACTLY_ONCE))
        val request = SubscribeRequest(0.toUShort(), subscriptions)
        assertEquals(12.toUInt(), request.payloadPacketSize)
    }

    @Test
    fun subscriptionPayload() {
        val subscriptions = Subscription.from(listOf("a/b", "c/d"), listOf(AT_LEAST_ONCE, EXACTLY_ONCE))
        val request = SubscribeRequest(0.toUShort(), subscriptions)
        val readPacket = request.payloadPacket()
        // Topic Filter ("a/b")
        // byte 1: Length MSB (0)
        assertEquals(0b00000000, readPacket.readByte())
        // byte2: Length LSB (3)
        assertEquals(0b00000011, readPacket.readByte())
        // byte3: a (0x61)
        assertEquals(0b01100001, readPacket.readByte())
        // byte4: / (0x2F)
        assertEquals(0b00101111, readPacket.readByte())
        // byte5: b (0x62)
        assertEquals(0b01100010, readPacket.readByte())
        // Subscription Options
        // byte6: Subscription Options (1)
        assertEquals(0b00000001, readPacket.readByte())


        // Topic Filter ("c/d")
        // byte 1: Length MSB (0)
        assertEquals(0b00000000, readPacket.readByte())
        // byte2: Length LSB (3)
        assertEquals(0b00000011, readPacket.readByte())
        // byte3: c (0x63)
        assertEquals(0b01100011, readPacket.readByte())
        // byte4: / (0x2F)
        assertEquals(0b00101111, readPacket.readByte())
        // byte5: d (0x64)
        assertEquals(0b01100100, readPacket.readByte())
        // Subscription Options
        // byte6: Subscription Options (2)
        assertEquals(0b00000010, readPacket.readByte())
        // No more bytes to read
        assertEquals(0, readPacket.remaining)
    }

    @Test
    fun serialized() {
        val subscriptions = Subscription.from(listOf("a/b", "c/d"), listOf(AT_LEAST_ONCE, EXACTLY_ONCE))
        val request = SubscribeRequest(10.toUShort(), subscriptions)
        val readPacket = request.serialize()
        // fixed header 2 bytes
        // byte 1 fixed header
//        assertEquals(13, readPacket.remaining)
        assertEquals(0b10000010.toUByte(), readPacket.readUByte())
        // byte 2 fixed header
//        assertEquals(12, readPacket.remaining)
        assertEquals(14.toUByte(), readPacket.readUByte())
        // Variable header 2 bytes
        // byte 1 variable header
//        assertEquals(11, readPacket.remaining)
        assertEquals(0b0.toUByte(), readPacket.readUByte())

        // byte 2 variable header
//        assertEquals(10, readPacket.remaining)
        assertEquals(12.toUByte(), readPacket.readUByte())

        // Payload 12 bytes
        // Topic Filter ("a/b")
        // byte 1: Length MSB (0)
        assertEquals(0b00000000, readPacket.readByte())
        // byte2: Length LSB (3)
        assertEquals(0b00000011, readPacket.readByte())
        // byte3: a (0x61)
        assertEquals(0b01100001, readPacket.readByte())
        // byte4: / (0x2F)
        assertEquals(0b00101111, readPacket.readByte())
        // byte5: b (0x62)
        assertEquals(0b01100010, readPacket.readByte())
        // Subscription Options
        // byte6: Subscription Options (1)
        assertEquals(0b00000001, readPacket.readByte())


        // Topic Filter ("c/d")
        // byte 1: Length MSB (0)
        assertEquals(0b00000000, readPacket.readByte())
        // byte2: Length LSB (3)
        assertEquals(0b00000011, readPacket.readByte())
        // byte3: c (0x63)
        assertEquals(0b01100011, readPacket.readByte())
        // byte4: / (0x2F)
        assertEquals(0b00101111, readPacket.readByte())
        // byte5: d (0x64)
        assertEquals(0b01100100, readPacket.readByte())
        // Subscription Options
        // byte6: Subscription Options (2)
        assertEquals(0b00000010, readPacket.readByte())
        // No more bytes to read
        assertEquals(0, readPacket.remaining)
    }

    @Test
    fun variableHeader() {
        val subscriptions = Subscription.from(listOf("1234567"), listOf(AT_LEAST_ONCE))
        val request = SubscribeRequest(10.toUShort(), subscriptions)
        val variableHeaderBytes = request.variableHeaderPacket
        assertEquals(2, variableHeaderBytes.remaining)
        assertEquals(10.toUShort(), variableHeaderBytes.readUShort())
    }

    @Test
    fun variableHeader2Subscriptions() {
        val subscriptions = Subscription.from(listOf("a/b", "c/d"), listOf(AT_LEAST_ONCE, EXACTLY_ONCE))
        val request = SubscribeRequest(10.toUShort(), subscriptions)
        val variableHeaderBytes = request.variableHeaderPacket
        assertEquals(2, variableHeaderBytes.remaining)
        assertEquals(12.toUShort(), variableHeaderBytes.readUShort())
    }

    @Test
    fun serializeWordSubscriptionTest() {
        val readPacket = Subscription(Filter("test")).packet
        assertEquals(7, readPacket.remaining)
        // Topic Filter ("test")
        // byte 1: Length MSB (0)
        assertEquals(0b00000000, readPacket.readByte())
        // byte2: Length LSB (4)
        assertEquals(0b00000100, readPacket.readByte())
        // byte3: t (0x74)
        assertEquals(0x74, readPacket.readByte())
        // byte4: e (0x65)
        assertEquals(0x65, readPacket.readByte())
        // byte5: s (0x73)
        assertEquals(0x73, readPacket.readByte())
        // byte6: t (0x74)
        assertEquals(0x74, readPacket.readByte())
        // Subscription Options
        // byte7: Subscription Options (1)
        assertEquals(0b00000001, readPacket.readByte())

    }

    @Test
    fun serializeDeserialize() {
        val subscribeRequest = SubscribeRequest(2.toUShort(), listOf(Subscription(Filter("test"))))
        assertEquals(subscribeRequest.packetIdentifier, 2.toUShort())
        val subs = subscribeRequest.subscriptions
        val firstSub = subs.first()
        val filter = firstSub.topicFilter
        val validated = filter.validate()!!
        assertEquals(validated.value.value, "test")
        val subscribeRequestData = subscribeRequest.serialize()
        val requestRead = ControlPacketV4.from(subscribeRequestData) as SubscribeRequest
        val subs1 = requestRead.subscriptions
        val firstSub1 = subs1.first()
        val filter1 = firstSub1.topicFilter
        val validated1 = filter1.validate()!!
        assertEquals(validated1.value.value, "test")
    }
}
