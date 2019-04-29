@file:Suppress("EXPERIMENTAL_API_USAGE")

package mqtt.wire5.control.packet

import kotlinx.io.core.buildPacket
import kotlinx.io.core.readBytes
import kotlinx.io.core.writeFully
import kotlinx.io.core.writeUByte
import mqtt.wire.MalformedPacketException
import mqtt.wire.ProtocolError
import mqtt.wire.control.packet.format.ReasonCode.*
import mqtt.wire.data.MqttUtf8String
import mqtt.wire.data.VariableByteInteger
import mqtt.wire5.control.packet.UnsubscribeAcknowledgment.VariableHeader
import mqtt.wire5.control.packet.format.variable.property.ReasonString
import mqtt.wire5.control.packet.format.variable.property.UserProperty
import mqtt.wire5.control.packet.format.variable.property.readProperties
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.fail

class UnsubscribeAcknowledgmentTests {
    private val packetIdentifier = 2.toUShort()
    @Test
    fun serializeDeserializeDefault() {
        val actual = UnsubscribeAcknowledgment(VariableHeader(packetIdentifier))
        val bytes = actual.serialize()
        val expected = ControlPacketV5.from(bytes)
        assertEquals(expected, actual)
    }

    @Test
    fun serializeDeserializeNoSubscriptionsExisted() {
        val actual = UnsubscribeAcknowledgment(VariableHeader(packetIdentifier), listOf(NO_SUBSCRIPTIONS_EXISTED))
        val bytes = actual.serialize()
        val expected = ControlPacketV5.from(bytes)
        assertEquals(expected, actual)
    }

    @Test
    fun serializeDeserializeUnspecifiedError() {
        val actual = UnsubscribeAcknowledgment(VariableHeader(packetIdentifier), listOf(UNSPECIFIED_ERROR))
        val bytes = actual.serialize()
        val expected = ControlPacketV5.from(bytes)
        assertEquals(expected, actual)
    }

    @Test
    fun serializeDeserializeImplementationSpecificError() {
        val actual = UnsubscribeAcknowledgment(VariableHeader(packetIdentifier), listOf(IMPLEMENTATION_SPECIFIC_ERROR))
        val bytes = actual.serialize()
        val expected = ControlPacketV5.from(bytes)
        assertEquals(expected, actual)
    }

    @Test
    fun serializeDeserializeNotAuthorized() {
        val actual = UnsubscribeAcknowledgment(VariableHeader(packetIdentifier), listOf(NOT_AUTHORIZED))
        val bytes = actual.serialize()
        val expected = ControlPacketV5.from(bytes)
        assertEquals(expected, actual)
    }

    @Test
    fun serializeDeserializeTopicFilterInvalid() {
        val actual = UnsubscribeAcknowledgment(VariableHeader(packetIdentifier), listOf(TOPIC_FILTER_INVALID))
        val bytes = actual.serialize()
        val expected = ControlPacketV5.from(bytes)
        assertEquals(expected, actual)
    }

    @Test
    fun emptyReasonCodesThrowsProtocolError() {
        try {
            UnsubscribeAcknowledgment(VariableHeader(packetIdentifier), listOf())
            fail()
        } catch (e: ProtocolError) {
        }
    }

    @Test
    fun serializeDeserializePacketIdentifierInUse() {
        val actual = UnsubscribeAcknowledgment(VariableHeader(packetIdentifier), listOf(PACKET_IDENTIFIER_IN_USE))
        val bytes = actual.serialize()
        val expected = ControlPacketV5.from(bytes)
        assertEquals(expected, actual)
    }

    @Test
    fun reasonString() {
        val props = VariableHeader.Properties(reasonString = MqttUtf8String("yolo"))
        val header = VariableHeader(packetIdentifier, properties = props)
        val actual = UnsubscribeAcknowledgment(header)
        val bytes = actual.serialize()
        val expected = ControlPacketV5.from(bytes) as UnsubscribeAcknowledgment
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
        val props = VariableHeader.Properties.from(
                setOf(UserProperty(MqttUtf8String("key"), MqttUtf8String("value")),
                        UserProperty(MqttUtf8String("key"), MqttUtf8String("value"))))
        val userPropertyResult = props.userProperty
        for ((key, value) in userPropertyResult) {
            assertEquals(key.getValueOrThrow(), "key")
            assertEquals(value.getValueOrThrow(), "value")
        }
        assertEquals(userPropertyResult.size, 1)

        val request = UnsubscribeAcknowledgment(VariableHeader(packetIdentifier, properties = props)).serialize()
        val requestRead = ControlPacketV5.from(request.copy()) as UnsubscribeAcknowledgment
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
            UnsubscribeAcknowledgment.from(packet)
            fail()
        } catch (e: MalformedPacketException) {
        }
    }
}
