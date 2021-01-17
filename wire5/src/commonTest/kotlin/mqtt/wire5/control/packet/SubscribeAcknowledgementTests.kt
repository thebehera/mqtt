@file:Suppress("EXPERIMENTAL_API_USAGE", "EXPERIMENTAL_UNSIGNED_LITERALS")

package mqtt.wire5.control.packet

import mqtt.buffer.allocateNewBuffer
import mqtt.wire.MalformedPacketException
import mqtt.wire.ProtocolError
import mqtt.wire.buffer.writeVariableByteInteger
import mqtt.wire.control.packet.format.ReasonCode.*
import mqtt.wire5.control.packet.SubscribeAcknowledgement.VariableHeader
import mqtt.wire5.control.packet.SubscribeAcknowledgement.VariableHeader.Properties.Companion.from
import mqtt.wire5.control.packet.format.variable.property.ReasonString
import mqtt.wire5.control.packet.format.variable.property.UserProperty
import mqtt.wire5.control.packet.format.variable.property.readProperties
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.fail

class SubscribeAcknowledgementTests {
    private val packetIdentifier = 2

    @Test
    fun packetIdentifier() {
        val payload = GRANTED_QOS_0
        val suback = SubscribeAcknowledgement(packetIdentifier.toUShort(), payload)
        val buffer = allocateNewBuffer(6u, limits)
        suback.serialize(buffer)
        buffer.resetForRead()
        val subackResult = ControlPacketV5.from(buffer) as SubscribeAcknowledgement
        assertEquals(subackResult.variable.packetIdentifier, packetIdentifier)
        assertEquals(subackResult.payload.first(), GRANTED_QOS_0)
    }

    @Test
    fun grantedQos1() {
        val payload = GRANTED_QOS_1
        val obj = SubscribeAcknowledgement(packetIdentifier.toUShort(), payload)
        val buffer = allocateNewBuffer(6u, limits)
        obj.serialize(buffer)
        buffer.resetForRead()
        val result = ControlPacketV5.from(buffer) as SubscribeAcknowledgement
        assertEquals(result.payload.first(), GRANTED_QOS_1)
    }

    @Test
    fun grantedQos2() {
        val payload = GRANTED_QOS_2
        val obj = SubscribeAcknowledgement(packetIdentifier.toUShort(), payload)
        val buffer = allocateNewBuffer(6u, limits)
        obj.serialize(buffer)
        buffer.resetForRead()
        val result = ControlPacketV5.from(buffer) as SubscribeAcknowledgement
        assertEquals(result.payload.first(), GRANTED_QOS_2)
    }

    @Test
    fun unspecifiedError() {
        val payload = UNSPECIFIED_ERROR
        val obj = SubscribeAcknowledgement(packetIdentifier.toUShort(), payload)
        val buffer = allocateNewBuffer(6u, limits)
        obj.serialize(buffer)
        buffer.resetForRead()
        val result = ControlPacketV5.from(buffer) as SubscribeAcknowledgement
        assertEquals(result.payload.first(), UNSPECIFIED_ERROR)
    }

    @Test
    fun implementationSpecificError() {
        val payload = IMPLEMENTATION_SPECIFIC_ERROR
        val obj = SubscribeAcknowledgement(packetIdentifier.toUShort(), payload)
        val buffer = allocateNewBuffer(6u, limits)
        obj.serialize(buffer)
        buffer.resetForRead()
        val result = ControlPacketV5.from(buffer) as SubscribeAcknowledgement
        assertEquals(result.payload.first(), IMPLEMENTATION_SPECIFIC_ERROR)
    }

    @Test
    fun notAuthorized() {
        val payload = NOT_AUTHORIZED
        val obj = SubscribeAcknowledgement(packetIdentifier.toUShort(), payload)
        val buffer = allocateNewBuffer(6u, limits)
        obj.serialize(buffer)
        buffer.resetForRead()
        val result = ControlPacketV5.from(buffer) as SubscribeAcknowledgement
        assertEquals(result.payload.first(), NOT_AUTHORIZED)
    }

    @Test
    fun topicFilterInvalid() {
        val payload = TOPIC_FILTER_INVALID
        val obj = SubscribeAcknowledgement(packetIdentifier.toUShort(), payload)
        val buffer = allocateNewBuffer(6u, limits)
        obj.serialize(buffer)
        buffer.resetForRead()
        val result = ControlPacketV5.from(buffer) as SubscribeAcknowledgement
        assertEquals(result.payload.first(), TOPIC_FILTER_INVALID)
    }

    @Test
    fun packetIdentifierInUse() {
        val payload = PACKET_IDENTIFIER_IN_USE
        val obj = SubscribeAcknowledgement(packetIdentifier.toUShort(), payload)
        val buffer = allocateNewBuffer(6u, limits)
        obj.serialize(buffer)
        buffer.resetForRead()
        val result = ControlPacketV5.from(buffer) as SubscribeAcknowledgement
        assertEquals(result.payload.first(), PACKET_IDENTIFIER_IN_USE)
    }

    @Test
    fun quotaExceeded() {
        val payload = QUOTA_EXCEEDED
        val obj = SubscribeAcknowledgement(packetIdentifier.toUShort(), payload)
        val buffer = allocateNewBuffer(6u, limits)
        obj.serialize(buffer)
        buffer.resetForRead()
        val result = ControlPacketV5.from(buffer) as SubscribeAcknowledgement
        assertEquals(result.payload.first(), QUOTA_EXCEEDED)
    }

    @Test
    fun sharedSubscriptionsNotSupported() {
        val payload = SHARED_SUBSCRIPTIONS_NOT_SUPPORTED
        val obj = SubscribeAcknowledgement(packetIdentifier.toUShort(), payload)
        val buffer = allocateNewBuffer(6u, limits)
        obj.serialize(buffer)
        buffer.resetForRead()
        val result = ControlPacketV5.from(buffer) as SubscribeAcknowledgement
        assertEquals(result.payload.first(), SHARED_SUBSCRIPTIONS_NOT_SUPPORTED)
    }

    @Test
    fun subscriptionIdentifiersNotSupported() {
        val payload = SUBSCRIPTION_IDENTIFIERS_NOT_SUPPORTED
        val obj = SubscribeAcknowledgement(packetIdentifier.toUShort(), payload)
        val buffer = allocateNewBuffer(6u, limits)
        obj.serialize(buffer)
        buffer.resetForRead()
        val result = ControlPacketV5.from(buffer) as SubscribeAcknowledgement
        assertEquals(result.payload.first(), SUBSCRIPTION_IDENTIFIERS_NOT_SUPPORTED)
    }

    @Test
    fun wildcardSubscriptionsNotSupported() {
        val payload = WILDCARD_SUBSCRIPTIONS_NOT_SUPPORTED
        val obj = SubscribeAcknowledgement(packetIdentifier.toUShort(), payload)
        val buffer = allocateNewBuffer(6u, limits)
        obj.serialize(buffer)
        buffer.resetForRead()
        val result = ControlPacketV5.from(buffer) as SubscribeAcknowledgement
        assertEquals(result.payload.first(), WILDCARD_SUBSCRIPTIONS_NOT_SUPPORTED)
    }

    @Test
    fun invalidVariableHeaderPayload() {
        val payload = MALFORMED_PACKET
        try {
            SubscribeAcknowledgement(packetIdentifier.toUShort(), payload)
            fail()
        } catch (e: ProtocolError) {
        }
    }

    @Test
    fun reasonString() {
        val props = VariableHeader.Properties(reasonString = "yolo")
        val actual = SubscribeAcknowledgement(packetIdentifier.toUShort(), props, GRANTED_QOS_1)
        val buffer = allocateNewBuffer(13u, limits)
        actual.serialize(buffer)
        buffer.resetForRead()
        val result = ControlPacketV5.from(buffer) as SubscribeAcknowledgement
        assertEquals("yolo", result.variable.properties.reasonString.toString())
    }

    @Test
    fun reasonStringMultipleTimesThrowsProtocolError() {
        val obj1 = ReasonString("yolo")
        val obj2 = obj1.copy()
        val buffer = allocateNewBuffer(15u, limits)
        buffer.writeVariableByteInteger(obj1.size() + obj2.size())
        obj1.write(buffer)
        obj2.write(buffer)
        buffer.resetForRead()
        assertFailsWith<ProtocolError> { PublishReceived.VariableHeader.Properties.from(buffer.readProperties()) }
    }


    @Test
    fun variableHeaderPropertyUserProperty() {
        val props = from(setOf(UserProperty("key", "value")))
        val userPropertyResult = props.userProperty
        for ((key, value) in userPropertyResult) {
            assertEquals(key, "key")
            assertEquals(value, "value")
        }
        assertEquals(userPropertyResult.size, 1)

        val request = SubscribeAcknowledgement(packetIdentifier.toUShort(), props, WILDCARD_SUBSCRIPTIONS_NOT_SUPPORTED)
        val buffer = allocateNewBuffer(19u, limits)
        request.serialize(buffer)
        buffer.resetForRead()
        val requestRead = ControlPacketV5.from(buffer) as SubscribeAcknowledgement
        val (key, value) = requestRead.variable.properties.userProperty.first()
        assertEquals("key", key.toString())
        assertEquals("value", value.toString())
    }

    @Test
    fun invalidReasonCode() {
        val variable = VariableHeader(packetIdentifier)
        val buffer = allocateNewBuffer(5u, limits)
        variable.serialize(buffer)
        buffer.write(BANNED.byte)
        buffer.resetForRead()
        assertFailsWith<MalformedPacketException> { SubscribeAcknowledgement.from(buffer, 4u) }
    }
}
