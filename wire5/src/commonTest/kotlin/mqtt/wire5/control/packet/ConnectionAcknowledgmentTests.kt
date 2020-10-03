@file:Suppress("EXPERIMENTAL_API_USAGE", "EXPERIMENTAL_UNSIGNED_LITERALS")

package mqtt.wire5.control.packet

import mqtt.buffer.GenericType
import mqtt.buffer.allocateNewBuffer
import mqtt.wire.MalformedPacketException
import mqtt.wire.ProtocolError
import mqtt.wire.control.packet.format.ReasonCode.*
import mqtt.wire.control.packet.format.fixed.get
import mqtt.wire.data.QualityOfService.AT_LEAST_ONCE
import mqtt.wire5.control.packet.ConnectionAcknowledgment.VariableHeader
import mqtt.wire5.control.packet.ConnectionAcknowledgment.VariableHeader.Properties
import mqtt.wire5.control.packet.format.variable.property.*
import kotlin.test.*

class ConnectionAcknowledgmentTests {
    @Test
    fun serializeDefaults() {
        val buffer = allocateNewBuffer(6u, limits)
        val actual = ConnectionAcknowledgment<Unit>()
        actual.serialize(buffer)
        buffer.resetForRead()
        // fixed header
        assertEquals(0b00100000.toUByte(), buffer.readUnsignedByte(), "byte1 fixed header")
        assertEquals(3u, buffer.readVariableByteInteger(), "byte2 fixed header remaining length")
        // variable header
        assertEquals(0, buffer.readByte(), "byte0 variable header session Present Flag")
        assertEquals(SUCCESS.byte, buffer.readUnsignedByte(), "byte1 variable header connect reason code")
        assertEquals(0u, buffer.readVariableByteInteger(), "property length")
    }

    @Test
    fun deserializeDefaults() {
        val buffer = allocateNewBuffer(5u, limits)
        // fixed header
        buffer.write(0b00100000.toUByte())
        buffer.writeVariableByteInteger(3u)
        // variable header
        buffer.write(0.toByte())
        buffer.write(SUCCESS.byte)
        buffer.writeVariableByteInteger(0u)
        buffer.resetForRead()
        assertEquals(ConnectionAcknowledgment<Unit>(), ControlPacketV5.from(buffer))
    }

    @Test
    fun bit0SessionPresentFalseFlags() {
        val buffer = allocateNewBuffer(3u, limits)
        val model = ConnectionAcknowledgment<Unit>()
        model.header.serialize(buffer)
        buffer.resetForRead()
        val sessionPresentBit = buffer.readUnsignedByte().get(0)
        assertFalse(sessionPresentBit)

        val buffer2 = allocateNewBuffer(5u, limits)
        model.serialize(buffer2)
        buffer2.resetForRead()
        val result = ControlPacketV5.from(buffer2) as ConnectionAcknowledgment<*>
        assertFalse(result.header.sessionPresent)
    }

    @Test
    fun bit0SessionPresentFlags() {
        val buffer = allocateNewBuffer(3u, limits)
        val model = ConnectionAcknowledgment<Unit>(VariableHeader(true))
        model.header.serialize(buffer)
        buffer.resetForRead()
        val sessionPresentBit = buffer.readUnsignedByte().get(0)
        assertTrue(sessionPresentBit)
    }

    @Test
    fun connectReasonCodeDefaultSuccess() {
        val buffer = allocateNewBuffer(3u, limits)
        val model = ConnectionAcknowledgment<Unit>()
        model.header.serialize(buffer)
        buffer.resetForRead()
        val sessionPresentBit = buffer.readUnsignedByte().get(0)
        assertFalse(sessionPresentBit)

        val buffer2 = allocateNewBuffer(5u, limits)
        model.serialize(buffer2)
        buffer2.resetForRead()
        val result = ControlPacketV5.from(buffer2) as ConnectionAcknowledgment<*>
        assertEquals(result.header.connectReason, SUCCESS)
    }

    @Test
    fun connectReasonCodeDefaultUnspecifiedError() {
        val buffer = allocateNewBuffer(5u, limits)
        val model = ConnectionAcknowledgment<Unit>(VariableHeader(connectReason = UNSPECIFIED_ERROR))
        model.serialize(buffer)
        buffer.resetForRead()
        val model2 = ControlPacketV5.from(buffer) as ConnectionAcknowledgment<*>
        assertEquals(model, model2)
        assertEquals(UNSPECIFIED_ERROR, model2.header.connectReason)
    }

    @Test
    fun sessionExpiryInterval() {
        val actual = ConnectionAcknowledgment<Unit>(VariableHeader(properties = Properties(4L)))
        val buffer = allocateNewBuffer(12u, limits)
        actual.serialize(buffer)
        buffer.resetForRead()
        // fixed header
        assertEquals(0b00100000.toUByte(), buffer.readUnsignedByte(), "byte1 fixed header")
        assertEquals(8u, buffer.readVariableByteInteger(), "byte2 fixed header remaining length")
        // variable header
        assertEquals(0, buffer.readByte(), "byte0 variable header session Present Flag")
        assertEquals(SUCCESS.byte, buffer.readUnsignedByte(), "byte1 variable header connect reason code")
        assertEquals(5u, buffer.readVariableByteInteger(), "property length")
        assertEquals(0x11, buffer.readByte())
        assertEquals(4.toUInt(), buffer.readUnsignedInt())
        buffer.resetForRead()
        val expected = ControlPacketV5.from(buffer) as ConnectionAcknowledgment<*>
        assertEquals(4L, expected.header.properties.sessionExpiryIntervalSeconds)
    }

    @Test
    fun sessionExpiryIntervalMultipleTimesThrowsProtocolError() {
        val obj1 = SessionExpiryInterval(4L)
        val obj2 = obj1.copy()
        val buffer = allocateNewBuffer(14u, limits)
        val size = obj1.size() + obj2.size()
        buffer.writeVariableByteInteger(size)
        obj1.write(buffer)
        obj2.write(buffer)
        buffer.resetForRead()
        try {
            Properties.from(buffer.readProperties())
            fail()
        } catch (e: ProtocolError) {
        }
    }

    @Test
    fun receiveMaximum() {
        val actual = ConnectionAcknowledgment<Unit>(VariableHeader(properties = Properties(receiveMaximum = 4)))
        val buffer = allocateNewBuffer(8u, limits)
        actual.serialize(buffer)
        buffer.resetForRead()
        val expected = ControlPacketV5.from(buffer) as ConnectionAcknowledgment<*>
        assertEquals(expected.header.properties.receiveMaximum, 4)
        assertEquals(expected, actual)
    }

    @Test
    fun receiveMaximumSetToZeroThrowsProtocolError() {
        val actual = ConnectionAcknowledgment<Unit>(VariableHeader(properties = Properties(receiveMaximum = 0)))
        val buffer = allocateNewBuffer(8u, limits)
        actual.serialize(buffer)
        buffer.resetForRead()
        try {
            ControlPacketV5.from(buffer)
            fail()
        } catch (e: ProtocolError) {
        }
    }

    @Test
    fun receiveMaximumMultipleTimesThrowsProtocolError() {
        val obj1 = ReceiveMaximum(4)
        val obj2 = obj1.copy()
        val buffer = allocateNewBuffer(7u, limits)
        val size = obj1.size() + obj1.size()
        buffer.writeVariableByteInteger(size)
        obj1.write(buffer)
        obj2.write(buffer)
        buffer.resetForRead()
        try {
            Properties.from(buffer.readProperties())
            fail()
        } catch (e: ProtocolError) {
        }
    }

    @Test
    fun maximumQos() {
        val actual = ConnectionAcknowledgment<Unit>(VariableHeader(properties = Properties(maximumQos = AT_LEAST_ONCE)))
        val buffer = allocateNewBuffer(8u, limits)
        actual.serialize(buffer)
        buffer.resetForRead()
        val expected = ControlPacketV5.from(buffer) as ConnectionAcknowledgment<*>
        assertEquals(expected.header.properties.maximumQos, AT_LEAST_ONCE)
        assertEquals(expected, actual)
    }

    @Test
    fun maximumQosMultipleTimesThrowsProtocolError() {
        val obj1 = MaximumQos(AT_LEAST_ONCE)
        val obj2 = obj1.copy()
        val buffer = allocateNewBuffer(5u, limits)
        val size = obj1.size() + obj2.size()
        buffer.writeVariableByteInteger(size)
        obj1.write(buffer)
        obj2.write(buffer)
        buffer.resetForRead()
        try {
            VariableHeader(properties = Properties.from(buffer.readProperties()))
            fail()
        } catch (e: ProtocolError) {
        }
    }

    @Test
    fun retainAvailableTrue() {
        val actual = ConnectionAcknowledgment<Unit>(VariableHeader(properties = Properties(retainAvailable = true)))
        val buffer = allocateNewBuffer(5u, limits)
        actual.serialize(buffer)
        buffer.resetForRead()
        val expected = ControlPacketV5.from(buffer) as ConnectionAcknowledgment<*>
        assertEquals(expected.header.properties.retainAvailable, true)
        assertEquals(expected, actual)
    }

    @Test
    fun retainAvailableFalse() {
        val actual = ConnectionAcknowledgment<Unit>(VariableHeader(properties = Properties(retainAvailable = false)))
        val buffer = allocateNewBuffer(7u, limits)
        actual.serialize(buffer)
        buffer.resetForRead()
        val expected = ControlPacketV5.from(buffer) as ConnectionAcknowledgment<*>
        assertEquals(expected.header.properties.retainAvailable, false)
        assertEquals(expected, actual)
    }

    @Test
    fun retainAvailableSendDefaults() {
        val actual = ConnectionAcknowledgment<Unit>()
        val buffer = allocateNewBuffer(5u, limits)
        actual.serialize(buffer)
        buffer.resetForRead()
        val expected = ControlPacketV5.from(buffer) as ConnectionAcknowledgment<*>
        assertEquals(expected.header.properties.retainAvailable, true)
        assertEquals(expected, actual)
    }


    @Test
    fun retainAvailableMultipleTimesThrowsProtocolError() {
        val obj1 = RetainAvailable(true)
        val obj2 = obj1.copy()
        val buffer = allocateNewBuffer(5u, limits)
        val size = obj1.size() + obj2.size()
        buffer.writeVariableByteInteger(size)
        obj1.write(buffer)
        obj2.write(buffer)
        buffer.resetForRead()
        try {
            Properties.from(buffer.readProperties())
            fail()
        } catch (e: ProtocolError) {
        }
    }

    @Test
    fun maximumPacketSize() {
        val actual = ConnectionAcknowledgment<Unit>(VariableHeader(properties = Properties(maximumPacketSize = 4)))
        val buffer = allocateNewBuffer(10u, limits)
        actual.serialize(buffer)
        buffer.resetForRead()
        val expected = ControlPacketV5.from(buffer) as ConnectionAcknowledgment<*>
        assertEquals(expected.header.properties.maximumPacketSize, 4)
        assertEquals(expected, actual)
    }

    @Test
    fun maximumPacketSizeSetToZeroThrowsProtocolError() {
        val actual = ConnectionAcknowledgment<Unit>(VariableHeader(properties = Properties(maximumPacketSize = 0)))
        val buffer = allocateNewBuffer(10u, limits)
        actual.serialize(buffer)
        buffer.resetForRead()
        try {
            ControlPacketV5.from(buffer)
            fail()
        } catch (e: ProtocolError) {
        }
    }

    @Test
    fun maximumPacketSizeMultipleTimesThrowsProtocolError() {
        val obj1 = MaximumPacketSize(4)
        val obj2 = obj1.copy()
        val buffer = allocateNewBuffer(11u, limits)
        val size = obj1.size() + obj2.size()
        buffer.writeVariableByteInteger(size)
        obj1.write(buffer)
        obj2.write(buffer)
        buffer.resetForRead()
        try {
            Properties.from(buffer.readProperties())
            fail()
        } catch (e: ProtocolError) {
        }
    }

    @Test
    fun assignedClientIdentifier() {
        val actual = ConnectionAcknowledgment<Unit>(
            VariableHeader(properties = Properties(assignedClientIdentifier = "yolo".toCharSequenceBuffer()))
        )
        val buffer = allocateNewBuffer(12u, limits)
        actual.serialize(buffer)
        buffer.resetForRead()
        val expected = ControlPacketV5.from(buffer) as ConnectionAcknowledgment<*>
        assertEquals(expected.header.properties.assignedClientIdentifier, "yolo".toCharSequenceBuffer())
        assertEquals(expected, actual)
    }

    @Test
    fun assignedClientIdentifierMultipleTimesThrowsProtocolError() {
        val obj1 = AssignedClientIdentifier("yolo")
        val obj2 = obj1.copy()
        val buffer = allocateNewBuffer(15u, limits)
        val size = obj1.size() + obj2.size()
        buffer.writeVariableByteInteger(size)
        obj1.write(buffer)
        obj2.write(buffer)
        buffer.resetForRead()
        try {
            Properties.from(buffer.readProperties())
            fail()
        } catch (e: ProtocolError) {
        }
    }

    @Test
    fun topicAliasMaximum() {
        val actual = ConnectionAcknowledgment<Unit>(
            VariableHeader(properties = Properties(topicAliasMaximum = 4))
        )
        val buffer = allocateNewBuffer(8u, limits)
        actual.serialize(buffer)
        buffer.resetForRead()
        val expected = ControlPacketV5.from(buffer) as ConnectionAcknowledgment<*>
        assertEquals(expected.header.properties.topicAliasMaximum, 4)
    }

    @Test
    fun topicAliasMaximumMultipleTimesThrowsProtocolError() {
        val obj1 = TopicAliasMaximum(4)
        val obj2 = obj1.copy()
        val buffer = allocateNewBuffer(7u, limits)
        val size = obj1.size() + obj2.size()
        buffer.writeVariableByteInteger(size)
        obj1.write(buffer)
        obj2.write(buffer)
        buffer.resetForRead()
        try {
            Properties.from(buffer.readProperties())
            fail()
        } catch (e: ProtocolError) {
        }
    }

    @Test
    fun reasonString() {
        val actual = ConnectionAcknowledgment<Unit>(
            VariableHeader(properties = Properties(reasonString = "yolo".toCharSequenceBuffer()))
        )
        val buffer = allocateNewBuffer(12u, limits)
        actual.serialize(buffer)
        buffer.resetForRead()
        val expected = ControlPacketV5.from(buffer) as ConnectionAcknowledgment<*>
        assertEquals(expected.header.properties.reasonString, "yolo".toCharSequenceBuffer())
        assertEquals(expected, actual)
    }

    @Test
    fun reasonStringMultipleTimesThrowsProtocolError() {
        val obj1 = ReasonString("yolo")
        val obj2 = obj1.copy()
        val buffer = allocateNewBuffer(15u, limits)
        val size = obj1.size() + obj2.size()
        buffer.writeVariableByteInteger(size)
        obj1.write(buffer)
        obj2.write(buffer)
        buffer.resetForRead()
        try {
            Properties.from(buffer.readProperties())
            fail()
        } catch (e: ProtocolError) {
        }
    }

    @Test
    fun variableHeaderPropertyUserProperty() {
        val props = Properties.from(setOf(UserProperty("key", "value")))
        val userPropertyResult = props.userProperty
        for ((key, value) in userPropertyResult) {
            assertEquals(key, "key")
            assertEquals(value, "value")
        }
        assertEquals(userPropertyResult.size, 1)

        val connack = ConnectionAcknowledgment(VariableHeader(properties = props))
        val buffer = allocateNewBuffer(18u, limits)
        connack.serialize(buffer)
        buffer.resetForRead()
        val requestRead = ControlPacketV5.from(buffer) as ConnectionAcknowledgment<*>
        val (key, value) = requestRead.header.properties.userProperty.first()
        assertEquals(key.toString(), "key")
        assertEquals(value.toString(), "value")
    }

    @Test
    fun wildcardSubscriptionAvailable() {
        val actual =
            ConnectionAcknowledgment<Unit>(VariableHeader(properties = Properties(supportsWildcardSubscriptions = false)))
        val buffer = allocateNewBuffer(7u, limits)
        actual.serialize(buffer)
        buffer.resetForRead()
        val expected = ControlPacketV5.from(buffer) as ConnectionAcknowledgment<*>
        assertEquals(expected.header.properties.supportsWildcardSubscriptions, false)
    }

    @Test
    fun wildcardSubscriptionAvailableDefaults() {
        val actual = ConnectionAcknowledgment<Unit>()
        val buffer = allocateNewBuffer(5u, limits)
        actual.serialize(buffer)
        buffer.resetForRead()
        val expected = ControlPacketV5.from(buffer) as ConnectionAcknowledgment<*>
        assertEquals(expected.header.properties.supportsWildcardSubscriptions, true)
    }

    @Test
    fun wildcardSubscriptionAvailableMultipleTimesThrowsProtocolError() {
        val obj1 = WildcardSubscriptionAvailable(true)
        val obj2 = obj1.copy()
        val buffer = allocateNewBuffer(5u, limits)
        val size = obj1.size() + obj2.size()
        buffer.writeVariableByteInteger(size)
        obj1.write(buffer)
        obj2.write(buffer)
        buffer.resetForRead()
        try {
            Properties.from(buffer.readProperties())
            fail()
        } catch (e: ProtocolError) {
        }
    }

    @Test
    fun subscriptionIdentifierAvailableDefaults() {
        val actual = ConnectionAcknowledgment<Unit>()
        val buffer = allocateNewBuffer(5u, limits)
        actual.serialize(buffer)
        buffer.resetForRead()
        val expected = ControlPacketV5.from(buffer) as ConnectionAcknowledgment<*>
        assertEquals(expected.header.properties.subscriptionIdentifiersAvailable, true)
    }

    @Test
    fun subscriptionIdentifierAvailable() {
        val actual =
            ConnectionAcknowledgment<Unit>(VariableHeader(properties = Properties(subscriptionIdentifiersAvailable = false)))
        val buffer = allocateNewBuffer(7u, limits)
        actual.serialize(buffer)
        buffer.resetForRead()
        val expected = ControlPacketV5.from(buffer) as ConnectionAcknowledgment<*>
        assertEquals(expected.header.properties.subscriptionIdentifiersAvailable, false)
    }

    @Test
    fun subscriptionIdentifierAvailableMultipleTimesThrowsProtocolError() {
        val obj1 = SubscriptionIdentifierAvailable(true)
        val obj2 = obj1.copy()
        val buffer = allocateNewBuffer(5u, limits)
        val size = obj1.size() + obj2.size()
        buffer.writeVariableByteInteger(size)
        obj1.write(buffer)
        obj2.write(buffer)
        buffer.resetForRead()
        try {
            Properties.from(buffer.readProperties())
            fail()
        } catch (e: ProtocolError) {
        }
    }

    @Test
    fun sharedSubscriptionAvailableDefaults() {
        val actual = ConnectionAcknowledgment<Unit>()
        val buffer = allocateNewBuffer(5u, limits)
        actual.serialize(buffer)
        buffer.resetForRead()
        val expected = ControlPacketV5.from(buffer) as ConnectionAcknowledgment<*>
        assertEquals(expected.header.properties.sharedSubscriptionAvailable, true)
    }

    @Test
    fun sharedSubscriptionAvailable() {
        val actual =
            ConnectionAcknowledgment<Unit>(VariableHeader(properties = Properties(sharedSubscriptionAvailable = false)))
        val buffer = allocateNewBuffer(7u, limits)
        actual.serialize(buffer)
        buffer.resetForRead()
        val expected = ControlPacketV5.from(buffer) as ConnectionAcknowledgment<*>
        assertEquals(expected.header.properties.sharedSubscriptionAvailable, false)
    }

    @Test
    fun sharedSubscriptionAvailableMultipleTimesThrowsProtocolError() {
        val obj1 = SharedSubscriptionAvailable(true)
        val obj2 = obj1.copy()
        val buffer = allocateNewBuffer(5u, limits)
        val size = obj1.size() + obj2.size()
        buffer.writeVariableByteInteger(size)
        obj1.write(buffer)
        obj2.write(buffer)
        buffer.resetForRead()
        try {
            Properties.from(buffer.readProperties())
            fail()
        } catch (e: ProtocolError) {
        }
    }

    @Test
    fun serverKeepAlive() {
        val actual = ConnectionAcknowledgment<Unit>(VariableHeader(properties = Properties(serverKeepAlive = 5)))
        val buffer = allocateNewBuffer(8u, limits)
        actual.serialize(buffer)
        buffer.resetForRead()
        val expected = ControlPacketV5.from(buffer) as ConnectionAcknowledgment<*>
        assertEquals(expected.header.properties.sharedSubscriptionAvailable, true)
    }

    @Test
    fun serverKeepAliveMultipleTimesThrowsProtocolError() {
        val obj1 = ServerKeepAlive(5)
        val obj2 = obj1.copy()
        val buffer = allocateNewBuffer(7u, limits)
        val size = obj1.size() + obj2.size()
        buffer.writeVariableByteInteger(size)
        obj1.write(buffer)
        obj2.write(buffer)
        buffer.resetForRead()
        try {
            Properties.from(buffer.readProperties())
            fail()
        } catch (e: ProtocolError) {
        }
    }

    @Test
    fun responseInformation() {
        val actual = ConnectionAcknowledgment<Unit>(
            VariableHeader(properties = Properties(responseInformation = "yolo"))
        )
        val buffer = allocateNewBuffer(12u, limits)
        actual.serialize(buffer)
        buffer.resetForRead()
        val expected = ControlPacketV5.from(buffer) as ConnectionAcknowledgment<*>
        assertEquals(expected.header.properties.responseInformation, "yolo".toCharSequenceBuffer())
    }

    @Test
    fun responseInformationMultipleTimesThrowsProtocolError() {
        val obj1 = ResponseInformation("yolo")
        val obj2 = obj1.copy()
        val buffer = allocateNewBuffer(15u, limits)
        val size = obj1.size() + obj2.size()
        buffer.writeVariableByteInteger(size)
        obj1.write(buffer)
        obj2.write(buffer)
        buffer.resetForRead()
        try {
            Properties.from(buffer.readProperties())
            fail()
        } catch (e: ProtocolError) {
        }
    }

    @Test
    fun serverReference() {
        val actual = ConnectionAcknowledgment<Unit>(
            VariableHeader(properties = Properties(serverReference = "yolo"))
        )
        val buffer = allocateNewBuffer(12u, limits)
        actual.serialize(buffer)
        buffer.resetForRead()
        val expected = ControlPacketV5.from(buffer) as ConnectionAcknowledgment<*>
        assertEquals(expected.header.properties.serverReference, "yolo".toCharSequenceBuffer())
    }

    @Test
    fun serverReferenceMultipleTimesThrowsProtocolError() {
        val obj1 = ServerReference("yolo")
        val obj2 = obj1.copy()
        val buffer = allocateNewBuffer(15u, limits)
        val size = obj1.size() + obj2.size()
        buffer.writeVariableByteInteger(size)
        obj1.write(buffer)
        obj2.write(buffer)
        buffer.resetForRead()
        try {
            Properties.from(buffer.readProperties())
            fail()
        } catch (e: ProtocolError) {
        }
    }


    @Test
    fun authenticationMethodAndData() {
        val actual = ConnectionAcknowledgment(
            VariableHeader(
                properties = Properties(
                    authentication =
                    Authentication("yolo", GenericType("1234", CharSequence::class))
                )
            )
        )
        val buffer = allocateNewBuffer(19u, limits)
        actual.serialize(buffer)
        buffer.resetForRead()
        val expected = ControlPacketV5.from(buffer) as ConnectionAcknowledgment<*>
        assertEquals(expected.header.properties.authentication?.method?.toString(), "yolo")
        assertEquals(expected.header.properties.authentication?.data, GenericType("1234", CharSequence::class))
    }

    @Test
    fun authenticationMethodMultipleTimesThrowsProtocolError() {
        val obj1 = AuthenticationMethod("yolo")
        val obj2 = obj1.copy()
        val buffer = allocateNewBuffer(15u, limits)
        val size = obj1.size() + obj2.size()
        buffer.writeVariableByteInteger(size)
        obj1.write(buffer)
        obj2.write(buffer)
        buffer.resetForRead()
        try {
            Properties.from(buffer.readProperties())
            fail()
        } catch (e: ProtocolError) {
        }
    }

    @Test
    fun authenticationDataMultipleTimesThrowsProtocolError() {
        val obj1 = AuthenticationData(GenericType("1234", CharSequence::class))
        val obj2 = obj1.copy()
        val buffer = allocateNewBuffer(15u, limits)
        val size = obj1.size() + obj2.size()
        buffer.writeVariableByteInteger(size)
        obj1.write(buffer)
        obj2.write(buffer)
        buffer.resetForRead()
        try {
            Properties.from(buffer.readProperties())
            fail()
        } catch (e: ProtocolError) {
        }
    }

    @Test
    fun invalidPropertyOnVariableHeaderThrowsMalformedPacketException() {
        val method = WillDelayInterval(3)
        try {
            Properties.from(listOf(method, method))
            fail()
        } catch (e: MalformedPacketException) {
        }
    }

    @Test
    fun connectionReasonByteOnVariableHeaderIsInvalidThrowsMalformedPacketException() {
        val buffer = allocateNewBuffer(2u, limits)
        buffer.write(1.toByte())
        buffer.write(SERVER_SHUTTING_DOWN.byte)
        buffer.resetForRead()
        try {
            VariableHeader.from(buffer, 2u)
            fail()
        } catch (e: MalformedPacketException) {
        }
    }
}
