@file:Suppress("EXPERIMENTAL_API_USAGE", "EXPERIMENTAL_UNSIGNED_LITERALS")

package mqtt.wire4.control.packet

import mqtt.buffer.allocateNewBuffer
import mqtt.wire.data.QualityOfService.*
import mqtt.wire.data.topic.Filter
import kotlin.test.Test
import kotlin.test.assertEquals

class SubscribeRequestTest {

    @Test
    fun serializeTestByteArray() {
        val readBuffer = allocateNewBuffer(12u, limits)
        val subscription = Subscription.from(listOf(Filter("a/b"), Filter("c/d")), listOf(AT_LEAST_ONCE, EXACTLY_ONCE))
        Subscription.writeMany(subscription, readBuffer)
        readBuffer.resetForRead()
        // Topic Filter ("a/b")
        // byte 1: Length MSB (0)
        assertEquals(0b00000000, readBuffer.readByte())
        // byte2: Length LSB (3)
        assertEquals(0b00000011, readBuffer.readByte())
        // byte3: a (0x61)
        assertEquals(0b01100001, readBuffer.readByte())
        // byte4: / (0x2F)
        assertEquals(0b00101111, readBuffer.readByte())
        // byte5: b (0x62)
        assertEquals(0b01100010, readBuffer.readByte())
        // Subscription Options
        // byte6: Subscription Options (1)
        assertEquals(0b00000001, readBuffer.readByte())


        // Topic Filter ("c/d")
        // byte 1: Length MSB (0)
        assertEquals(0b00000000, readBuffer.readByte())
        // byte2: Length LSB (3)
        assertEquals(0b00000011, readBuffer.readByte())
        // byte3: c (0x63)
        assertEquals(0b01100011, readBuffer.readByte())
        // byte4: / (0x2F)
        assertEquals(0b00101111, readBuffer.readByte())
        // byte5: d (0x64)
        assertEquals(0b01100100, readBuffer.readByte())
        // Subscription Options
        // byte6: Subscription Options (2)
        assertEquals(0b00000010, readBuffer.readByte())
    }

    @Test
    fun subscriptionPayloadSize() {
        val subscriptions = Subscription.from(listOf(Filter("a/b"), Filter("c/d")), listOf(AT_LEAST_ONCE, EXACTLY_ONCE))
        val request = SubscribeRequest(0, subscriptions)
        assertEquals(12.toUInt(), request.payloadPacketSize)
    }

    @Test
    fun subscriptionPayload() {
        val readBuffer = allocateNewBuffer(12u, limits)
        val subscription = Subscription.from(listOf(Filter("a/b"), Filter("c/d")), listOf(AT_LEAST_ONCE, EXACTLY_ONCE))
        Subscription.writeMany(subscription, readBuffer)
        readBuffer.resetForRead()
        // Topic Filter ("a/b")
        // byte 1: Length MSB (0)
        assertEquals(0b00000000, readBuffer.readByte())
        // byte2: Length LSB (3)
        assertEquals(0b00000011, readBuffer.readByte())
        // byte3: a (0x61)
        assertEquals(0b01100001, readBuffer.readByte())
        // byte4: / (0x2F)
        assertEquals(0b00101111, readBuffer.readByte())
        // byte5: b (0x62)
        assertEquals(0b01100010, readBuffer.readByte())
        // Subscription Options
        // byte6: Subscription Options (1)
        assertEquals(0b00000001, readBuffer.readByte())


        // Topic Filter ("c/d")
        // byte 1: Length MSB (0)
        assertEquals(0b00000000, readBuffer.readByte())
        // byte2: Length LSB (3)
        assertEquals(0b00000011, readBuffer.readByte())
        // byte3: c (0x63)
        assertEquals(0b01100011, readBuffer.readByte())
        // byte4: / (0x2F)
        assertEquals(0b00101111, readBuffer.readByte())
        // byte5: d (0x64)
        assertEquals(0b01100100, readBuffer.readByte())
        // Subscription Options
        // byte6: Subscription Options (2)
        assertEquals(0b00000010, readBuffer.readByte())
    }

    @Test
    fun packetIdentifierIsCorrect() {
        val buffer = allocateNewBuffer(10u, limits)
        val subscription = SubscribeRequest(10.toUShort(), Filter("a/b"), AT_MOST_ONCE)
        assertEquals(10, subscription.packetIdentifier)
        subscription.serialize(buffer)
        buffer.resetForRead()
        buffer.readByte()
        buffer.readByte()
        val packetIdentifer = buffer.readUnsignedShort()
        assertEquals(10.toUShort(), packetIdentifer)
    }

    @Test
    fun serialized() {
        val subscriptions = Subscription.from(listOf(Filter("a/b"), Filter("c/d")), listOf(AT_LEAST_ONCE, EXACTLY_ONCE))
        val buffer = allocateNewBuffer(19u, limits)
        val request = SubscribeRequest(10, subscriptions)
        request.serialize(buffer)
        buffer.resetForRead()
        // fixed header 2 bytes
        // byte 1 fixed header
        assertEquals(0b10000010.toUByte(), buffer.readUnsignedByte())
        // byte 2 fixed header
        assertEquals(14.toUByte(), buffer.readUnsignedByte())
        // Variable header 2 bytes
        // byte 1 variable header
//        assertEquals(11, readPacket.remaining)
        assertEquals(0b0.toUByte(), buffer.readUnsignedByte())

        // byte 2 variable header
//        assertEquals(10, readPacket.remaining)
        assertEquals(10.toUByte(), buffer.readUnsignedByte())

        // Payload 12 bytes
        // Topic Filter ("a/b")
        // byte 1: Length MSB (0)
        assertEquals(0b00000000, buffer.readByte())
        // byte2: Length LSB (3)
        assertEquals(0b00000011, buffer.readByte())
        // byte3: a (0x61)
        assertEquals(0b01100001, buffer.readByte())
        // byte4: / (0x2F)
        assertEquals(0b00101111, buffer.readByte())
        // byte5: b (0x62)
        assertEquals(0b01100010, buffer.readByte())
        // Subscription Options
        // byte6: Subscription Options (1)
        assertEquals(0b00000001, buffer.readByte())


        // Topic Filter ("c/d")
        // byte 1: Length MSB (0)
        assertEquals(0b00000000, buffer.readByte())
        // byte2: Length LSB (3)
        assertEquals(0b00000011, buffer.readByte())
        // byte3: c (0x63)
        assertEquals(0b01100011, buffer.readByte())
        // byte4: / (0x2F)
        assertEquals(0b00101111, buffer.readByte())
        // byte5: d (0x64)
        assertEquals(0b01100100, buffer.readByte())
        // Subscription Options
        // byte6: Subscription Options (2)
        assertEquals(0b00000010, buffer.readByte())
    }

    @Test
    fun serializeDeserialize() {
        val subscribeRequest = SubscribeRequest(2, listOf(Subscription(Filter("test"))))
        assertEquals(subscribeRequest.packetIdentifier, 2)
        val subs = subscribeRequest.subscriptions
        val firstSub = subs.first()
        val filter = firstSub.topicFilter
        val validated = filter.validate()!!
        assertEquals(validated.level.value, "test")
        val buffer = allocateNewBuffer(11u, limits)
        subscribeRequest.serialize(buffer)
        buffer.resetForRead()
        val requestRead = ControlPacketV4.from(buffer) as SubscribeRequest
        val subs1 = requestRead.subscriptions
        val firstSub1 = subs1.first()
        val filter1 = firstSub1.topicFilter
        val validated1 = filter1.validate()!!
        assertEquals(validated1.level.value, "test")
    }
}
