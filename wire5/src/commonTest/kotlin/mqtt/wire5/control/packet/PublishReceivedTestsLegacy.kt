@file:Suppress("EXPERIMENTAL_API_USAGE")

package mqtt.wire5.control.packet

import kotlinx.io.core.buildPacket
import kotlinx.io.core.readBytes
import kotlinx.io.core.writeFully
import mqtt.wire.ProtocolError
import mqtt.wire.control.packet.format.ReasonCode.*
import mqtt.wire.data.MqttUtf8String
import mqtt.wire.data.VariableByteInteger
import mqtt.wire5.control.packet.PublishReceived.VariableHeader
import mqtt.wire5.control.packet.format.variable.property.ReasonString
import mqtt.wire5.control.packet.format.variable.property.UserProperty
import mqtt.wire5.control.packet.format.variable.property.readPropertiesLegacy
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.fail

class PublishReceivedTestsLegacy {
    private val packetIdentifier = 2

    @Test
    fun packetIdentifier() {
        val puback = PublishReceived(VariableHeader(packetIdentifier))
        val data = puback.serialize()
        val pubackResult = ControlPacketV5.from(data) as PublishReceived
        assertEquals(pubackResult.variable.packetIdentifier, packetIdentifier)
    }

    @Test
    fun packetIdentifierSendDefaults() {
        val puback = PublishReceived(VariableHeader(packetIdentifier))
        val data = puback.serialize(true)
        val pubackResult = ControlPacketV5.from(data) as PublishReceived
        assertEquals(pubackResult.variable.packetIdentifier, packetIdentifier)
    }

    @Test
    fun noMatchingSubscribers() {
        val puback = PublishReceived(VariableHeader(packetIdentifier, NO_MATCHING_SUBSCRIBERS))
        val data = puback.serialize()
        val pubackResult = ControlPacketV5.from(data) as PublishReceived
        assertEquals(pubackResult.variable.reasonCode, NO_MATCHING_SUBSCRIBERS)
    }

    @Test
    fun unspecifiedError() {
        val puback = PublishReceived(VariableHeader(packetIdentifier, UNSPECIFIED_ERROR))
        val data = puback.serialize()
        val pubackResult = ControlPacketV5.from(data) as PublishReceived
        assertEquals(pubackResult.variable.reasonCode, UNSPECIFIED_ERROR)
    }

    @Test
    fun implementationSpecificError() {
        val puback = PublishReceived(VariableHeader(packetIdentifier, IMPLEMENTATION_SPECIFIC_ERROR))
        val data = puback.serialize()
        val pubackResult = ControlPacketV5.from(data) as PublishReceived
        assertEquals(pubackResult.variable.reasonCode, IMPLEMENTATION_SPECIFIC_ERROR)
    }

    @Test
    fun notAuthorized() {
        val puback = PublishReceived(VariableHeader(packetIdentifier, NOT_AUTHORIZED))
        val data = puback.serialize()
        val pubackResult = ControlPacketV5.from(data) as PublishReceived
        assertEquals(pubackResult.variable.reasonCode, NOT_AUTHORIZED)
    }

    @Test
    fun topicNameInvalid() {
        val puback = PublishReceived(VariableHeader(packetIdentifier, TOPIC_NAME_INVALID))
        val data = puback.serialize()
        val pubackResult = ControlPacketV5.from(data) as PublishReceived
        assertEquals(pubackResult.variable.reasonCode, TOPIC_NAME_INVALID)
    }

    @Test
    fun packetIdentifierInUse() {
        val puback = PublishReceived(VariableHeader(packetIdentifier, PACKET_IDENTIFIER_IN_USE))
        val data = puback.serialize()
        val pubackResult = ControlPacketV5.from(data) as PublishReceived
        assertEquals(pubackResult.variable.reasonCode, PACKET_IDENTIFIER_IN_USE)
    }

    @Test
    fun quotaExceeded() {
        val puback = PublishReceived(VariableHeader(packetIdentifier, QUOTA_EXCEEDED))
        val data = puback.serialize()
        val pubackResult = ControlPacketV5.from(data) as PublishReceived
        assertEquals(pubackResult.variable.reasonCode, QUOTA_EXCEEDED)
    }

    @Test
    fun payloadFormatInvalid() {
        val puback = PublishReceived(VariableHeader(packetIdentifier, PAYLOAD_FORMAT_INVALID))
        val data = puback.serialize()
        val pubackResult = ControlPacketV5.from(data) as PublishReceived
        assertEquals(pubackResult.variable.reasonCode, PAYLOAD_FORMAT_INVALID)
    }

    @Test
    fun invalidReasonCodeThrowsProtocolError() {
        try {
            PublishReceived(VariableHeader(packetIdentifier, RECEIVE_MAXIMUM_EXCEEDED))
            fail()
        } catch (e: ProtocolError) {
        }
    }


    @Test
    fun reasonString() {
        val actual = PublishReceived(VariableHeader(packetIdentifier, properties = VariableHeader.Properties(reasonString = MqttUtf8String("yolo"))))
        val bytes = actual.serialize()
        val expected = ControlPacketV5.from(bytes) as PublishReceived
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
            VariableHeader.Properties.from(props.readPropertiesLegacy())
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

        val request = PublishReceived(VariableHeader(packetIdentifier, properties = props)).serialize()
        val requestRead = ControlPacketV5.from(request.copy()) as PublishReceived
        val (key, value) = requestRead.variable.properties.userProperty.first()
        assertEquals(key.getValueOrThrow(), "key")
        assertEquals(value.getValueOrThrow(), "value")
    }

}