@file:Suppress("EXPERIMENTAL_API_USAGE")

package mqtt.wire5.control.packet

import kotlinx.io.core.*
import mqtt.wire.MalformedPacketException
import mqtt.wire.ProtocolError
import mqtt.wire.control.packet.format.ReasonCode.*
import mqtt.wire.control.packet.format.fixed.get
import mqtt.wire.data.ByteArrayWrapper
import mqtt.wire.data.MqttUtf8String
import mqtt.wire.data.QualityOfService.AT_LEAST_ONCE
import mqtt.wire.data.VariableByteInteger
import mqtt.wire5.control.packet.ConnectionAcknowledgment.VariableHeader
import mqtt.wire5.control.packet.ConnectionAcknowledgment.VariableHeader.Properties
import mqtt.wire5.control.packet.ConnectionAcknowledgment.VariableHeader.Properties.Authentication
import mqtt.wire5.control.packet.format.variable.property.*
import kotlin.test.*

class ConnectionAcknowledgmentTests {
    @Test
    fun serializeDeserializeDefault() {
        val actual = ConnectionAcknowledgment()
        val bytes = actual.serialize()
        val expected = ControlPacketV5.from(bytes)
        assertEquals(expected, actual)
    }

    @Test
    fun bit0SessionPresentFalseFlags() {
        val model = ConnectionAcknowledgment()
        val data = model.header.packet().readBytes()
        val sessionPresentBit = data[0].toUByte().get(0)
        assertFalse(sessionPresentBit)
        val result = ControlPacketV5.from(model.serialize()) as ConnectionAcknowledgment
        assertFalse(result.header.sessionPresent)
    }

    @Test
    fun bit0SessionPresentFlags() {
        val model = ConnectionAcknowledgment(VariableHeader(true))
        val data = model.header.packet().readBytes()
        val sessionPresentBit = data[0].toUByte().get(0)
        assertTrue(sessionPresentBit)
    }

    @Test
    fun connectReasonCodeDefaultSuccess() {
        val model = ConnectionAcknowledgment()
        val headerData = model.header.packet().readBytes()
        val connectReasonByte = headerData[1].toUByte()
        assertEquals(connackConnectReason[connectReasonByte], SUCCESS)
        val allData = model.serialize()
        val result = ControlPacketV5.from(allData) as ConnectionAcknowledgment
        assertEquals(result.header.connectReason, SUCCESS)
    }

    @Test
    fun connectReasonCodeDefaultUnspecifiedError() {
        val model = ConnectionAcknowledgment(VariableHeader(connectReason = UNSPECIFIED_ERROR))
        val data = model.header.packet().readBytes()
        val connectReasonByte = data[1].toUByte()
        assertEquals(connackConnectReason[connectReasonByte], UNSPECIFIED_ERROR)
    }

    @Test
    fun sessionExpiryInterval() {
        val actual = ConnectionAcknowledgment(VariableHeader(properties = Properties(4.toUInt())))
        val bytes = actual.serialize()
        val expected = ControlPacketV5.from(bytes) as ConnectionAcknowledgment
        assertEquals(expected.header.properties.sessionExpiryIntervalSeconds, 4.toUInt())
    }

    @Test
    fun sessionExpiryIntervalMultipleTimesThrowsProtocolError() {
        val obj1 = SessionExpiryInterval(4.toUInt())
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
            Properties.from(props.readProperties())
            fail()
        } catch (e: ProtocolError) {
        }
    }

    @Test
    fun receiveMaximum() {
        val actual = ConnectionAcknowledgment(VariableHeader(properties = Properties(receiveMaximum = 4.toUShort())))
        val bytes = actual.serialize()
        val expected = ControlPacketV5.from(bytes) as ConnectionAcknowledgment
        assertEquals(expected.header.properties.receiveMaximum, 4.toUShort())
    }

    @Test
    fun receiveMaximumSetToZeroThrowsProtocolError() {
        val actual = ConnectionAcknowledgment(VariableHeader(properties = Properties(receiveMaximum = 0.toUShort())))
        val bytes = actual.serialize()
        try {
            ControlPacketV5.from(bytes)
            fail()
        } catch (e: ProtocolError) {
        }
    }

    @Test
    fun receiveMaximumMultipleTimesThrowsProtocolError() {
        val obj1 = ReceiveMaximum(4.toUShort())
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
            Properties.from(props.readProperties())
            fail()
        } catch (e: ProtocolError) {
        }
    }

    @Test
    fun maximumQos() {
        val actual = ConnectionAcknowledgment(VariableHeader(properties = Properties(maximumQos = AT_LEAST_ONCE)))
        val bytes = actual.serialize()
        val expected = ControlPacketV5.from(bytes) as ConnectionAcknowledgment
        assertEquals(expected.header.properties.maximumQos, AT_LEAST_ONCE)
    }

    @Test
    fun maximumQosMultipleTimesThrowsProtocolError() {
        val obj1 = MaximumQos(AT_LEAST_ONCE)
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
            Properties.from(props.readProperties())
            fail()
        } catch (e: ProtocolError) {
        }
    }

    @Test
    fun retainAvailableTrue() {
        val actual = ConnectionAcknowledgment(VariableHeader(properties = Properties(retainAvailable = true)))
        val bytes = actual.serialize()
        val expected = ControlPacketV5.from(bytes) as ConnectionAcknowledgment
        assertEquals(expected.header.properties.retainAvailable, true)
    }

    @Test
    fun retainAvailableFalse() {
        val actual = ConnectionAcknowledgment(VariableHeader(properties = Properties(retainAvailable = false)))
        val bytes = actual.serialize()
        val expected = ControlPacketV5.from(bytes) as ConnectionAcknowledgment
        assertEquals(expected.header.properties.retainAvailable, false)
    }

    @Test
    fun retainAvailableSendDefaults() {
        val actual = ConnectionAcknowledgment()
        val bytes = actual.serialize(true)
        val expected = ControlPacketV5.from(bytes) as ConnectionAcknowledgment
        assertEquals(expected.header.properties.retainAvailable, true)
    }


    @Test
    fun retainAvailableMultipleTimesThrowsProtocolError() {
        val obj1 = RetainAvailable(true)
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
            Properties.from(props.readProperties())
            fail()
        } catch (e: ProtocolError) {
        }
    }

    @Test
    fun maximumPacketSize() {
        val actual = ConnectionAcknowledgment(
                VariableHeader(properties = Properties(maximumPacketSize = 4.toUInt())))
        val bytes = actual.serialize()
        val expected = ControlPacketV5.from(bytes) as ConnectionAcknowledgment
        assertEquals(expected.header.properties.maximumPacketSize, 4.toUInt())
    }

    @Test
    fun maximumPacketSizeSetToZeroThrowsProtocolError() {
        val actual = ConnectionAcknowledgment(VariableHeader(properties = Properties(maximumPacketSize = 0.toUInt())))
        val bytes = actual.serialize()
        try {
            ControlPacketV5.from(bytes)
            fail()
        } catch (e: ProtocolError) {
        }
    }

    @Test
    fun maximumPacketSizeMultipleTimesThrowsProtocolError() {
        val obj1 = MaximumPacketSize(4.toUInt())
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
            Properties.from(props.readProperties())
            fail()
        } catch (e: ProtocolError) {
        }
    }

    @Test
    fun assignedClientIdentifier() {
        val actual = ConnectionAcknowledgment(
                VariableHeader(properties = Properties(assignedClientIdentifier = MqttUtf8String("yolo"))))
        val bytes = actual.serialize()
        val expected = ControlPacketV5.from(bytes) as ConnectionAcknowledgment
        assertEquals(expected.header.properties.assignedClientIdentifier, MqttUtf8String("yolo"))
    }

    @Test
    fun assignedClientIdentifierMultipleTimesThrowsProtocolError() {
        val obj1 = AssignedClientIdentifier(MqttUtf8String("yolo"))
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
            Properties.from(props.readProperties())
            fail()
        } catch (e: ProtocolError) {
        }
    }

    @Test
    fun topicAliasMaximum() {
        val actual = ConnectionAcknowledgment(
                VariableHeader(properties = Properties(topicAliasMaximum = 4.toUShort())))
        val bytes = actual.serialize()
        val expected = ControlPacketV5.from(bytes) as ConnectionAcknowledgment
        assertEquals(expected.header.properties.topicAliasMaximum, 4.toUShort())
    }

    @Test
    fun topicAliasMaximumMultipleTimesThrowsProtocolError() {
        val obj1 = TopicAliasMaximum(4.toUShort())
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
            Properties.from(props.readProperties())
            fail()
        } catch (e: ProtocolError) {
        }
    }

    @Test
    fun reasonString() {
        val actual = ConnectionAcknowledgment(
                VariableHeader(properties = Properties(reasonString = MqttUtf8String("yolo"))))
        val bytes = actual.serialize()
        val expected = ControlPacketV5.from(bytes) as ConnectionAcknowledgment
        assertEquals(expected.header.properties.reasonString, MqttUtf8String("yolo"))
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
            Properties.from(props.readProperties())
            fail()
        } catch (e: ProtocolError) {
        }
    }

    @Test
    fun variableHeaderPropertyUserProperty() {
        val props = Properties.from(setOf(UserProperty(MqttUtf8String("key"), MqttUtf8String("value"))))
        val userPropertyResult = props.userProperty
        for ((key, value) in userPropertyResult) {
            assertEquals(key.getValueOrThrow(), "key")
            assertEquals(value.getValueOrThrow(), "value")
        }
        assertEquals(userPropertyResult.size, 1)

        val request = ConnectionAcknowledgment(VariableHeader(properties = props)).serialize()
        val requestRead = ControlPacketV5.from(request.copy()) as ConnectionAcknowledgment
        val (key, value) = requestRead.header.properties.userProperty.first()
        assertEquals(key.getValueOrThrow(), "key")
        assertEquals(value.getValueOrThrow(), "value")
    }

    @Test
    fun wildcardSubscriptionAvailable() {
        val actual = ConnectionAcknowledgment(VariableHeader(properties = Properties(supportsWildcardSubscriptions = false)))
        val bytes = actual.serialize()
        val expected = ControlPacketV5.from(bytes) as ConnectionAcknowledgment
        assertEquals(expected.header.properties.supportsWildcardSubscriptions, false)
    }

    @Test
    fun wildcardSubscriptionAvailableDefaults() {
        val actual = ConnectionAcknowledgment()
        val bytes = actual.serialize(true)
        val expected = ControlPacketV5.from(bytes) as ConnectionAcknowledgment
        assertEquals(expected.header.properties.supportsWildcardSubscriptions, true)
    }

    @Test
    fun wildcardSubscriptionAvailableMultipleTimesThrowsProtocolError() {
        val obj1 = WildcardSubscriptionAvailable(true)
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
            Properties.from(props.readProperties())
            fail()
        } catch (e: ProtocolError) {
        }
    }

    @Test
    fun subscriptionIdentifierAvailableDefaults() {
        val actual = ConnectionAcknowledgment()
        val bytes = actual.serialize()
        val expected = ControlPacketV5.from(bytes) as ConnectionAcknowledgment
        assertEquals(expected.header.properties.subscriptionIdentifiersAvailable, true)
    }

    @Test
    fun subscriptionIdentifierAvailable() {
        val actual = ConnectionAcknowledgment(VariableHeader(properties = Properties(subscriptionIdentifiersAvailable = false)))
        val bytes = actual.serialize()
        val expected = ControlPacketV5.from(bytes) as ConnectionAcknowledgment
        assertEquals(expected.header.properties.subscriptionIdentifiersAvailable, false)
    }

    @Test
    fun subscriptionIdentifierAvailableMultipleTimesThrowsProtocolError() {
        val obj1 = SubscriptionIdentifierAvailable(true)
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
            Properties.from(props.readProperties())
            fail()
        } catch (e: ProtocolError) {
        }
    }

    @Test
    fun sharedSubscriptionAvailableDefaults() {
        val actual = ConnectionAcknowledgment()
        val bytes = actual.serialize()
        val expected = ControlPacketV5.from(bytes) as ConnectionAcknowledgment
        assertEquals(expected.header.properties.sharedSubscriptionAvailable, true)
    }

    @Test
    fun sharedSubscriptionAvailable() {
        val actual = ConnectionAcknowledgment(VariableHeader(properties = Properties(sharedSubscriptionAvailable = false)))
        val bytes = actual.serialize()
        val expected = ControlPacketV5.from(bytes) as ConnectionAcknowledgment
        assertEquals(expected.header.properties.sharedSubscriptionAvailable, false)
    }

    @Test
    fun sharedSubscriptionAvailableMultipleTimesThrowsProtocolError() {
        val obj1 = SharedSubscriptionAvailable(true)
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
            Properties.from(props.readProperties())
            fail()
        } catch (e: ProtocolError) {
        }
    }

    @Test
    fun serverKeepAlive() {
        val actual = ConnectionAcknowledgment(VariableHeader(properties = Properties(serverKeepAlive = 5.toUShort())))
        val bytes = actual.serialize()
        val expected = ControlPacketV5.from(bytes) as ConnectionAcknowledgment
        assertEquals(expected.header.properties.sharedSubscriptionAvailable, true)
    }

    @Test
    fun serverKeepAliveMultipleTimesThrowsProtocolError() {
        val obj1 = ServerKeepAlive(5.toUShort())
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
            Properties.from(props.readProperties())
            fail()
        } catch (e: ProtocolError) {
        }
    }

    @Test
    fun responseInformation() {
        val actual = ConnectionAcknowledgment(
                VariableHeader(properties = Properties(responseInformation = MqttUtf8String("yolo"))))
        val bytes = actual.serialize()
        val expected = ControlPacketV5.from(bytes) as ConnectionAcknowledgment
        assertEquals(expected.header.properties.responseInformation, MqttUtf8String("yolo"))
    }

    @Test
    fun responseInformationMultipleTimesThrowsProtocolError() {
        val obj1 = ResponseInformation(MqttUtf8String("yolo"))
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
            Properties.from(props.readProperties())
            fail()
        } catch (e: ProtocolError) {
        }
    }

    @Test
    fun serverReference() {
        val actual = ConnectionAcknowledgment(
                VariableHeader(properties = Properties(serverReference = MqttUtf8String("yolo"))))
        val bytes = actual.serialize()
        val expected = ControlPacketV5.from(bytes) as ConnectionAcknowledgment
        assertEquals(expected.header.properties.serverReference, MqttUtf8String("yolo"))
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
            Properties.from(props.readProperties())
            fail()
        } catch (e: ProtocolError) {
        }
    }


    @Test
    fun authenticationMethodAndData() {
        val actual = ConnectionAcknowledgment(
                VariableHeader(properties = Properties(authentication =
                Authentication(MqttUtf8String("yolo"), ByteArrayWrapper("yolo".toByteArray())))))
        val bytes = actual.serialize()
        val expected = ControlPacketV5.from(bytes) as ConnectionAcknowledgment
        assertEquals(expected.header.properties.authentication?.method?.getValueOrThrow(), "yolo")
        assertEquals(expected.header.properties.authentication?.data, ByteArrayWrapper("yolo".toByteArray()))
    }

    @Test
    fun authenticationMethodMultipleTimesThrowsProtocolError() {
        val obj1 = AuthenticationMethod(MqttUtf8String("yolo"))
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
            Properties.from(props.readProperties())
            fail()
        } catch (e: ProtocolError) {
        }
    }

    @Test
    fun authenticationDataMultipleTimesThrowsProtocolError() {
        val obj1 = AuthenticationData(ByteArrayWrapper("yolo".toByteArray()))
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
            Properties.from(props.readProperties())
            fail()
        } catch (e: ProtocolError) {
        }
    }

    @Test
    fun invalidPropertyOnVariableHeaderThrowsMalformedPacketException() {
        val method = WillDelayInterval(3.toUInt())
        try {
            Properties.from(listOf(method, method))
            fail()
        } catch (e: MalformedPacketException) {
        }
    }

    @Test
    fun connectionReasonByteOnVariableHeaderIsInvalidThrowsMalformedPacketException() {
        val buffer = buildPacket {
            writeByte(1)
            writeUByte(SERVER_SHUTTING_DOWN.byte)
        }
        try {
            VariableHeader.from(buffer)
            fail()
        } catch (e: MalformedPacketException) {
        }
    }
}
