@file:Suppress("EXPERIMENTAL_API_USAGE")

package mqtt.wire5.control.packet

import kotlinx.io.core.buildPacket
import kotlinx.io.core.readBytes
import kotlinx.io.core.writeFully
import kotlinx.io.core.writeUByte
import mqtt.wire.MalformedPacketException
import mqtt.wire.ProtocolError
import mqtt.wire.data.MqttUtf8String
import mqtt.wire.data.VariableByteInteger
import mqtt.wire5.control.packet.SubscribeAcknowledgement.VariableHeader
import mqtt.wire5.control.packet.format.ReasonCode.*
import mqtt.wire5.control.packet.format.variable.property.ReasonString
import mqtt.wire5.control.packet.format.variable.property.UserProperty
import mqtt.wire5.control.packet.format.variable.property.readProperties
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.fail

class SubscribeAcknowledgementTests {
    private val packetIdentifier = 2.toUShort()

    @Test
    fun packetIdentifier() {
        val variableHeader = VariableHeader(packetIdentifier)
        val payload = GRANTED_QOS_0
        val puback = SubscribeAcknowledgement(variableHeader, payload)
        val data = puback.serialize()
        val pubackResult = ControlPacketV5.from(data) as SubscribeAcknowledgement
        assertEquals(pubackResult.variable.packetIdentifier, packetIdentifier)
        assertEquals(pubackResult.payload, GRANTED_QOS_0)
    }

    @Test
    fun grantedQos1() {
        val variableHeader = VariableHeader(packetIdentifier)
        val payload = GRANTED_QOS_1
        val obj = SubscribeAcknowledgement(variableHeader, payload)
        val result = ControlPacketV5.from(obj.serialize()) as SubscribeAcknowledgement
        assertEquals(result.payload, GRANTED_QOS_1)
    }

    @Test
    fun grantedQos2() {
        val variableHeader = VariableHeader(packetIdentifier)
        val payload = GRANTED_QOS_2
        val obj = SubscribeAcknowledgement(variableHeader, payload)
        val result = ControlPacketV5.from(obj.serialize()) as SubscribeAcknowledgement
        assertEquals(result.payload, GRANTED_QOS_2)
    }

    @Test
    fun unspecifiedError() {
        val variableHeader = VariableHeader(packetIdentifier)
        val payload = UNSPECIFIED_ERROR
        val obj = SubscribeAcknowledgement(variableHeader, payload)
        val result = ControlPacketV5.from(obj.serialize()) as SubscribeAcknowledgement
        assertEquals(result.payload, UNSPECIFIED_ERROR)
    }

    @Test
    fun implementationSpecificError() {
        val variableHeader = VariableHeader(packetIdentifier)
        val payload = IMPLEMENTATION_SPECIFIC_ERROR
        val obj = SubscribeAcknowledgement(variableHeader, payload)
        val result = ControlPacketV5.from(obj.serialize()) as SubscribeAcknowledgement
        assertEquals(result.payload, IMPLEMENTATION_SPECIFIC_ERROR)
    }

    @Test
    fun notAuthorized() {
        val variableHeader = VariableHeader(packetIdentifier)
        val payload = NOT_AUTHORIZED
        val obj = SubscribeAcknowledgement(variableHeader, payload)
        val result = ControlPacketV5.from(obj.serialize()) as SubscribeAcknowledgement
        assertEquals(result.payload, NOT_AUTHORIZED)
    }

    @Test
    fun topicFilterInvalid() {
        val variableHeader = VariableHeader(packetIdentifier)
        val payload = TOPIC_FILTER_INVALID
        val obj = SubscribeAcknowledgement(variableHeader, payload)
        val result = ControlPacketV5.from(obj.serialize()) as SubscribeAcknowledgement
        assertEquals(result.payload, TOPIC_FILTER_INVALID)
    }

    @Test
    fun packetIdentifierInUse() {
        val variableHeader = VariableHeader(packetIdentifier)
        val payload = PACKET_IDENTIFIER_IN_USE
        val obj = SubscribeAcknowledgement(variableHeader, payload)
        val result = ControlPacketV5.from(obj.serialize()) as SubscribeAcknowledgement
        assertEquals(result.payload, PACKET_IDENTIFIER_IN_USE)
    }

    @Test
    fun quotaExceeded() {
        val variableHeader = VariableHeader(packetIdentifier)
        val payload = QUOTA_EXCEEDED
        val obj = SubscribeAcknowledgement(variableHeader, payload)
        val result = ControlPacketV5.from(obj.serialize()) as SubscribeAcknowledgement
        assertEquals(result.payload, QUOTA_EXCEEDED)
    }

    @Test
    fun sharedSubscriptionsNotSupported() {
        val variableHeader = VariableHeader(packetIdentifier)
        val payload = SHARED_SUBSCRIPTIONS_NOT_SUPPORTED
        val obj = SubscribeAcknowledgement(variableHeader, payload)
        val result = ControlPacketV5.from(obj.serialize()) as SubscribeAcknowledgement
        assertEquals(result.payload, SHARED_SUBSCRIPTIONS_NOT_SUPPORTED)
    }

    @Test
    fun subscriptionIdentifiersNotSupported() {
        val variableHeader = VariableHeader(packetIdentifier)
        val payload = SUBSCRIPTION_IDENTIFIERS_NOT_SUPPORTED
        val obj = SubscribeAcknowledgement(variableHeader, payload)
        val result = ControlPacketV5.from(obj.serialize()) as SubscribeAcknowledgement
        assertEquals(result.payload, SUBSCRIPTION_IDENTIFIERS_NOT_SUPPORTED)
    }

    @Test
    fun wildcardSubscriptionsNotSupported() {
        val variableHeader = VariableHeader(packetIdentifier)
        val payload = WILDCARD_SUBSCRIPTIONS_NOT_SUPPORTED
        val obj = SubscribeAcknowledgement(variableHeader, payload)
        val result = ControlPacketV5.from(obj.serialize()) as SubscribeAcknowledgement
        assertEquals(result.payload, WILDCARD_SUBSCRIPTIONS_NOT_SUPPORTED)
    }

    @Test
    fun invalidVariableHeaderPayload() {
        val variableHeader = VariableHeader(packetIdentifier)
        val payload = MALFORMED_PACKET
        try {
            SubscribeAcknowledgement(variableHeader, payload)
            fail()
        } catch (e: ProtocolError) {
        }
    }

    @Test
    fun reasonString() {
        val props = VariableHeader.Properties(reasonString = MqttUtf8String("yolo"))
        val header = VariableHeader(packetIdentifier, properties = props)
        val actual = SubscribeAcknowledgement(header, GRANTED_QOS_1)
        val bytes = actual.serialize()
        val expected = ControlPacketV5.from(bytes) as SubscribeAcknowledgement
        assertEquals(expected.variable.properties.reasonString, MqttUtf8String("yolo"))
    }

    @Test
    fun reasonStringMultipleTimesThrowsProtocolError() {
        val obj1 = ReasonString(MqttUtf8String("yolo"))
        val obj2 = obj1.copy()
        val propsWithoutPropertyLength = buildPacket {
            obj1.write(this)
            obj2.write(this)
        }.readBytes()
        val props = buildPacket {
            writePacket(VariableByteInteger(propsWithoutPropertyLength.size.toUInt()).encodedValue())
            writeFully(propsWithoutPropertyLength)
        }.copy()
        try {
            VariableHeader.Properties.from(props.readProperties())
            fail()
        } catch (e: ProtocolError) {
        }
    }


    @Test
    fun variableHeaderPropertyUserProperty() {
        val props = VariableHeader.Properties.from(setOf(UserProperty(MqttUtf8String("key"), MqttUtf8String("value"))))
        val userPropertyResult = props.userProperty
        for ((key, value) in userPropertyResult) {
            assertEquals(key.getValueOrThrow(), "key")
            assertEquals(value.getValueOrThrow(), "value")
        }
        assertEquals(userPropertyResult.size, 1)

        val request = SubscribeAcknowledgement(VariableHeader(packetIdentifier, properties = props), WILDCARD_SUBSCRIPTIONS_NOT_SUPPORTED).serialize()
        val requestRead = ControlPacketV5.from(request.copy()) as SubscribeAcknowledgement
        val (key, value) = requestRead.variable.properties.userProperty.first()
        assertEquals(key.getValueOrThrow(), "key")
        assertEquals(value.getValueOrThrow(), "value")
    }

    @Test
    fun invalidReasonCode() {
        val variable = VariableHeader(packetIdentifier)
        val packet = buildPacket {
            writePacket(variable.packet)
            writeUByte(BANNED.byte)
        }
        try {
            SubscribeAcknowledgement.from(packet)
        } catch (e: MalformedPacketException) {
        }
    }
}
