@file:Suppress("EXPERIMENTAL_API_USAGE", "EXPERIMENTAL_UNSIGNED_LITERALS")

package mqtt.wire5.control.packet

import kotlinx.io.core.buildPacket
import kotlinx.io.core.readBytes
import kotlinx.io.core.writeFully
import kotlinx.io.core.writeUByte
import mqtt.buffer.allocateNewBuffer
import mqtt.wire.MalformedPacketException
import mqtt.wire.ProtocolError
import mqtt.wire.control.packet.format.ReasonCode.*
import mqtt.wire.data.MqttUtf8String
import mqtt.wire.data.VariableByteInteger
import mqtt.wire5.control.packet.DisconnectNotification.VariableHeader
import mqtt.wire5.control.packet.DisconnectNotification.VariableHeader.Properties
import mqtt.wire5.control.packet.format.variable.property.*
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.fail

class DisconnectTests {
    @Test
    fun sessionExpiryInterval() {
        val expected = DisconnectNotification(VariableHeader(properties = Properties(4)))
        val buffer = allocateNewBuffer(9u, limits)
        expected.serialize(buffer)
        buffer.resetForRead()
        val actual = ControlPacketV5.from(buffer) as DisconnectNotification
        assertEquals(4, actual.variable.properties.sessionExpiryIntervalSeconds)
        assertEquals(expected, actual)
    }

    @Test
    fun sessionExpiryIntervalMultipleTimesThrowsProtocolError() {
        val obj1 = SessionExpiryInterval(4)
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
            Properties.from(props.readPropertiesLegacy())
            fail()
        } catch (e: ProtocolError) {
        }
    }

    @Test
    fun reasonString() {
        val props = Properties(reasonString = MqttUtf8String("yolo"))
        val header = VariableHeader(NORMAL_DISCONNECTION, properties = props)
        val actual = DisconnectNotification(header)
        val bytes = actual.serialize()
        val expected = ControlPacketV5.from(bytes) as DisconnectNotification
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
            Properties.from(props.readPropertiesLegacy())
            fail()
        } catch (e: ProtocolError) {
        }
    }

    @Test
    fun variableHeaderPropertyUserProperty() {
        val props = Properties.from(
                setOf(UserProperty(MqttUtf8String("key"), MqttUtf8String("value")),
                        UserProperty(MqttUtf8String("key"), MqttUtf8String("value"))))
        val userPropertyResult = props.userProperty
        for ((key, value) in userPropertyResult) {
            assertEquals(key.getValueOrThrow(), "key")
            assertEquals(value.getValueOrThrow(), "value")
        }
        assertEquals(userPropertyResult.size, 1)

        val request = DisconnectNotification(VariableHeader(NORMAL_DISCONNECTION, properties = props)).serialize()
        val requestRead = ControlPacketV5.from(request.copy()) as DisconnectNotification
        val (key, value) = requestRead.variable.properties.userProperty.first()
        assertEquals(key.getValueOrThrow(), "key")
        assertEquals(value.getValueOrThrow(), "value")
    }

    @Test
    fun invalidReasonCode() {
        val variable = VariableHeader(NORMAL_DISCONNECTION)
        val packet = buildPacket {
            writePacket(variable.packet)
            writeUByte(BANNED.byte)
        }
        try {
            DisconnectNotification.from(packet)
        } catch (e: MalformedPacketException) {
        }
    }

    @Test
    fun serverReference() {
        val actual = DisconnectNotification(
                VariableHeader(properties = Properties(serverReference = MqttUtf8String("yolo"))))
        val bytes = actual.serialize()
        val expected = ControlPacketV5.from(bytes) as DisconnectNotification
        assertEquals(expected.variable.properties.serverReference, MqttUtf8String("yolo"))
    }

    @Test
    fun serverReferenceMultipleTimesThrowsProtocolError() {
        val obj1 = ServerReference(MqttUtf8String("yolo"))
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
            Properties.from(props.readPropertiesLegacy())
            fail()
        } catch (e: ProtocolError) {
        }
    }

    @Test
    fun invalidPropertyThrowsException() {
        val pairs = setOf<Property>(WillDelayInterval(3))
        try {
            Properties.from(pairs)
            fail()
        } catch (e: MalformedPacketException) {
        }
    }

    @Test
    fun serializeDeserializeDefaults() {
        val disconnect = DisconnectNotification(VariableHeader())
        val deserialized = ControlPacketV5.from(disconnect.serialize()) as DisconnectNotification
        assertEquals(deserialized.variable.reasonCode, NORMAL_DISCONNECTION)
        assertEquals(disconnect, deserialized)
    }

    @Test
    fun serializeDeserializeNormalDisconnection() {
        val disconnect = DisconnectNotification(VariableHeader(NORMAL_DISCONNECTION))
        val deserialized = ControlPacketV5.from(disconnect.serialize()) as DisconnectNotification
        assertEquals(deserialized.variable.reasonCode, NORMAL_DISCONNECTION)
        assertEquals(disconnect, deserialized)
    }

    @Test
    fun serializeDeserializeDisconnectWithWillMessage() {
        val reason = DISCONNECT_WITH_WILL_MESSAGE
        val disconnect = DisconnectNotification(VariableHeader(reason))
        val deserialized = ControlPacketV5.from(disconnect.serialize()) as DisconnectNotification
        assertEquals(deserialized.variable.reasonCode, reason)
        assertEquals(disconnect, deserialized)
    }

    @Test
    fun serializeDeserializeUnspecifiedError() {
        val reason = UNSPECIFIED_ERROR
        val disconnect = DisconnectNotification(VariableHeader(reason))
        val deserialized = ControlPacketV5.from(disconnect.serialize()) as DisconnectNotification
        assertEquals(deserialized.variable.reasonCode, reason)
        assertEquals(disconnect, deserialized)
    }

    @Test
    fun serializeDeserializeMalformedPacket() {
        val reason = MALFORMED_PACKET
        val disconnect = DisconnectNotification(VariableHeader(reason))
        val deserialized = ControlPacketV5.from(disconnect.serialize()) as DisconnectNotification
        assertEquals(deserialized.variable.reasonCode, reason)
        assertEquals(disconnect, deserialized)
    }

    @Test
    fun serializeDeserializeProtocolError() {
        val reason = PROTOCOL_ERROR
        val disconnect = DisconnectNotification(VariableHeader(reason))
        val deserialized = ControlPacketV5.from(disconnect.serialize()) as DisconnectNotification
        assertEquals(deserialized.variable.reasonCode, reason)
        assertEquals(disconnect, deserialized)
    }

    @Test
    fun serializeDeserializeImplementationSpecificError() {
        val reason = IMPLEMENTATION_SPECIFIC_ERROR
        val disconnect = DisconnectNotification(VariableHeader(IMPLEMENTATION_SPECIFIC_ERROR))
        val deserialized = ControlPacketV5.from(disconnect.serialize()) as DisconnectNotification
        assertEquals(deserialized.variable.reasonCode, reason)
        assertEquals(disconnect, deserialized)
    }

    @Test
    fun serializeDeserializeNotAuthorized() {
        val reason = NOT_AUTHORIZED
        val disconnect = DisconnectNotification(VariableHeader(reason))
        val deserialized = ControlPacketV5.from(disconnect.serialize()) as DisconnectNotification
        assertEquals(deserialized.variable.reasonCode, reason)
        assertEquals(disconnect, deserialized)
    }

    @Test
    fun serializeDeserializeServerBusy() {
        val reason = SERVER_BUSY
        val disconnect = DisconnectNotification(VariableHeader(reason))
        val deserialized = ControlPacketV5.from(disconnect.serialize()) as DisconnectNotification
        assertEquals(deserialized.variable.reasonCode, reason)
        assertEquals(disconnect, deserialized)
    }

    @Test
    fun serializeDeserializeServerShuttingDown() {
        val reason = SERVER_SHUTTING_DOWN
        val disconnect = DisconnectNotification(VariableHeader(reason))
        val deserialized = ControlPacketV5.from(disconnect.serialize()) as DisconnectNotification
        assertEquals(deserialized.variable.reasonCode, reason)
        assertEquals(disconnect, deserialized)
    }

    @Test
    fun serializeDeserializeKeepAliveTimeout() {
        val reason = KEEP_ALIVE_TIMEOUT
        val disconnect = DisconnectNotification(VariableHeader(reason))
        val deserialized = ControlPacketV5.from(disconnect.serialize()) as DisconnectNotification
        assertEquals(deserialized.variable.reasonCode, reason)
        assertEquals(disconnect, deserialized)
    }

    @Test
    fun serializeDeserializeSessionTakeOver() {
        val reason = SESSION_TAKE_OVER
        val disconnect = DisconnectNotification(VariableHeader(reason))
        val deserialized = ControlPacketV5.from(disconnect.serialize()) as DisconnectNotification
        assertEquals(deserialized.variable.reasonCode, reason)
        assertEquals(disconnect, deserialized)
    }

    @Test
    fun serializeDeserializeTopicFilterInvalid() {
        val reason = TOPIC_FILTER_INVALID
        val disconnect = DisconnectNotification(VariableHeader(reason))
        val deserialized = ControlPacketV5.from(disconnect.serialize()) as DisconnectNotification
        assertEquals(deserialized.variable.reasonCode, reason)
        assertEquals(disconnect, deserialized)
    }

    @Test
    fun serializeDeserializeTopicNameInvalid() {
        val reason = TOPIC_NAME_INVALID
        val disconnect = DisconnectNotification(VariableHeader(reason))
        val deserialized = ControlPacketV5.from(disconnect.serialize()) as DisconnectNotification
        assertEquals(deserialized.variable.reasonCode, reason)
        assertEquals(disconnect, deserialized)
    }

    @Test
    fun serializeDeserializeReceiveMaximumExceeded() {
        val reason = RECEIVE_MAXIMUM_EXCEEDED
        val disconnect = DisconnectNotification(VariableHeader(reason))
        val deserialized = ControlPacketV5.from(disconnect.serialize()) as DisconnectNotification
        assertEquals(deserialized.variable.reasonCode, reason)
        assertEquals(disconnect, deserialized)
    }

    @Test
    fun serializeDeserializeTopicAliasInvalid() {
        val reason = TOPIC_ALIAS_INVALID
        val disconnect = DisconnectNotification(VariableHeader(reason))
        val deserialized = ControlPacketV5.from(disconnect.serialize()) as DisconnectNotification
        assertEquals(deserialized.variable.reasonCode, reason)
        assertEquals(disconnect, deserialized)
    }

    @Test
    fun serializeDeserializePacketTooLarge() {
        val reason = PACKET_TOO_LARGE
        val disconnect = DisconnectNotification(VariableHeader(reason))
        val deserialized = ControlPacketV5.from(disconnect.serialize()) as DisconnectNotification
        assertEquals(deserialized.variable.reasonCode, reason)
        assertEquals(disconnect, deserialized)
    }

    @Test
    fun serializeDeserializeMessageRateTooHigh() {
        val reason = MESSAGE_RATE_TOO_HIGH
        val disconnect = DisconnectNotification(VariableHeader(reason))
        val deserialized = ControlPacketV5.from(disconnect.serialize()) as DisconnectNotification
        assertEquals(deserialized.variable.reasonCode, reason)
        assertEquals(disconnect, deserialized)
    }

    @Test
    fun serializeDeserializeQuotaExceeded() {
        val reason = QUOTA_EXCEEDED
        val disconnect = DisconnectNotification(VariableHeader(reason))
        val deserialized = ControlPacketV5.from(disconnect.serialize()) as DisconnectNotification
        assertEquals(deserialized.variable.reasonCode, reason)
        assertEquals(disconnect, deserialized)
    }

    @Test
    fun serializeDeserializeAdministrativeAction() {
        val reason = ADMINISTRATIVE_ACTION
        val disconnect = DisconnectNotification(VariableHeader(reason))
        val deserialized = ControlPacketV5.from(disconnect.serialize()) as DisconnectNotification
        assertEquals(deserialized.variable.reasonCode, reason)
        assertEquals(disconnect, deserialized)
    }

    @Test
    fun serializeDeserializePayloadFormatInvalid() {
        val reason = PAYLOAD_FORMAT_INVALID
        val disconnect = DisconnectNotification(VariableHeader(reason))
        val deserialized = ControlPacketV5.from(disconnect.serialize()) as DisconnectNotification
        assertEquals(deserialized.variable.reasonCode, reason)
        assertEquals(disconnect, deserialized)
    }


    @Test
    fun serializeDeserializeRetainNotSupported() {
        val reason = RETAIN_NOT_SUPPORTED
        val disconnect = DisconnectNotification(VariableHeader(reason))
        val deserialized = ControlPacketV5.from(disconnect.serialize()) as DisconnectNotification
        assertEquals(deserialized.variable.reasonCode, reason)
        assertEquals(disconnect, deserialized)
    }

    @Test
    fun serializeDeserializeQosNotSupported() {
        val reason = QOS_NOT_SUPPORTED
        val disconnect = DisconnectNotification(VariableHeader(reason))
        val deserialized = ControlPacketV5.from(disconnect.serialize()) as DisconnectNotification
        assertEquals(deserialized.variable.reasonCode, reason)
        assertEquals(disconnect, deserialized)
    }

    @Test
    fun serializeDeserializeUseAnotherServer() {
        val reason = USE_ANOTHER_SERVER
        val disconnect = DisconnectNotification(VariableHeader(reason))
        val deserialized = ControlPacketV5.from(disconnect.serialize()) as DisconnectNotification
        assertEquals(deserialized.variable.reasonCode, reason)
        assertEquals(disconnect, deserialized)
    }


    @Test
    fun serializeDeserializeServerMoved() {
        val reason = SERVER_MOVED
        val disconnect = DisconnectNotification(VariableHeader(reason))
        val deserialized = ControlPacketV5.from(disconnect.serialize()) as DisconnectNotification
        assertEquals(deserialized.variable.reasonCode, reason)
        assertEquals(disconnect, deserialized)
    }

    @Test
    fun serializeDeserializeSharedSubscriptionNotSupported() {
        val reason = SHARED_SUBSCRIPTIONS_NOT_SUPPORTED
        val disconnect = DisconnectNotification(VariableHeader(reason))
        val deserialized = ControlPacketV5.from(disconnect.serialize()) as DisconnectNotification
        assertEquals(deserialized.variable.reasonCode, reason)
        assertEquals(disconnect, deserialized)
    }

    @Test
    fun serializeDeserializeConnectionRateExceeded() {
        val reason = CONNECTION_RATE_EXCEEDED
        val disconnect = DisconnectNotification(VariableHeader(reason))
        val deserialized = ControlPacketV5.from(disconnect.serialize()) as DisconnectNotification
        assertEquals(deserialized.variable.reasonCode, reason)
        assertEquals(disconnect, deserialized)
    }

    @Test
    fun serializeDeserializeMaximumConnectionTime() {
        val reason = MAXIMUM_CONNECTION_TIME
        val disconnect = DisconnectNotification(VariableHeader(reason))
        val deserialized = ControlPacketV5.from(disconnect.serialize()) as DisconnectNotification
        assertEquals(deserialized.variable.reasonCode, reason)
        assertEquals(disconnect, deserialized)
    }

    @Test
    fun serializeDeserializeSubscriptionIdentifiersNotSupported() {
        val reason = SUBSCRIPTION_IDENTIFIERS_NOT_SUPPORTED
        val disconnect = DisconnectNotification(VariableHeader(reason))
        val deserialized = ControlPacketV5.from(disconnect.serialize()) as DisconnectNotification
        assertEquals(deserialized.variable.reasonCode, reason)
        assertEquals(disconnect, deserialized)
    }

    @Test
    fun serializeDeserializeWildcardSubscriptionsNotSupported() {
        val reason = WILDCARD_SUBSCRIPTIONS_NOT_SUPPORTED
        val disconnect = DisconnectNotification(VariableHeader(reason))
        val deserialized = ControlPacketV5.from(disconnect.serialize()) as DisconnectNotification
        assertEquals(deserialized.variable.reasonCode, reason)
        assertEquals(disconnect, deserialized)
    }

    @Test
    fun serializeDeserializeInvalid() {
        try {
            VariableHeader(BANNED)
            fail()
        } catch (e: MalformedPacketException) {
        }
    }
}
