@file:Suppress("EXPERIMENTAL_API_USAGE", "EXPERIMENTAL_UNSIGNED_LITERALS")

package mqtt.wire5.control.packet

import mqtt.buffer.GenericType
import mqtt.buffer.allocateNewBuffer
import mqtt.wire.MalformedPacketException
import mqtt.wire.ProtocolError
import mqtt.wire.data.QualityOfService
import mqtt.wire5.control.packet.PublishMessage.FixedHeader
import mqtt.wire5.control.packet.PublishMessage.VariableHeader
import mqtt.wire5.control.packet.format.variable.property.*
import kotlin.test.*

class PublishMessageTests {

    @Test
    fun serialize() {
        val buffer = allocateNewBuffer(8u, limits)
        val expected = PublishMessage<Unit>("", QualityOfService.AT_LEAST_ONCE, 1u)
        expected.serialize(buffer)
        buffer.resetForRead()
        assertEquals(0b00110010, buffer.readByte(), "fixed header byte 1")
        assertEquals(5u, buffer.readVariableByteInteger(), "fixed header remaining length")
        assertEquals("", buffer.readMqttUtf8StringNotValidated().toString(), "topic name")
        assertEquals(1u, buffer.readUnsignedShort(), "packet identifier")
        assertEquals(0, buffer.readProperties()?.count() ?: 0, "properties")
        buffer.resetForRead()
        val actual = ControlPacketV5.from(buffer) as PublishMessage<*>
        assertEquals(expected, actual)
    }

    @Test
    fun qosBothBitsSetTo1ThrowsMalformedPacketException() {
        val byte1 = 0b00111110.toByte()
        val remainingLength = 1u
        val buffer = allocateNewBuffer(2u, limits)
        buffer.write(byte1)
        buffer.writeVariableByteInteger(remainingLength)
        buffer.resetForRead()
        try {
            ControlPacketV5.from(buffer) as PublishMessage<*>
            fail()
        } catch (e: MalformedPacketException) {
        }
    }

    @Test
    fun payloadFormatIndicatorDefault() {
        val buffer = allocateNewBuffer(6u, limits)
        val expected = PublishMessage<Unit>(variable = VariableHeader("t"))
        expected.serialize(buffer)
        buffer.resetForRead()
        assertEquals(0b00110000, buffer.readByte(), "fixed header byte 1")
        assertEquals(4u, buffer.readVariableByteInteger(), "fixed header remaining length")
        assertEquals("t", buffer.readMqttUtf8StringNotValidated().toString(), "topic name")
        assertEquals(0, buffer.readProperties()?.count() ?: 0, "properties")
        buffer.resetForRead()
        val publish = ControlPacketV5.from(buffer) as PublishMessage<*>
        assertFalse(publish.variable.properties.payloadFormatIndicator)
    }

    @Test
    fun payloadFormatIndicatorTrue() {
        val props = VariableHeader.Properties<Any>(true)
        val variableHeader = VariableHeader("t", properties = props)
        val buffer = allocateNewBuffer(8u, limits)
        val expected = PublishMessage<Unit>(variable = variableHeader)
        expected.serialize(buffer)
        buffer.resetForRead()
        assertEquals(0b00110000, buffer.readByte(), "fixed header byte 1")
        assertEquals(6u, buffer.readVariableByteInteger(), "fixed header remaining length")
        assertEquals("t", buffer.readMqttUtf8StringNotValidated().toString(), "topic name")
        val propertiesActual = buffer.readProperties()
        assertEquals(1, propertiesActual?.count() ?: 0, "properties")
        assertEquals(
            props.payloadFormatIndicator,
            (propertiesActual?.first() as PayloadFormatIndicator).willMessageIsUtf8
        )
        buffer.resetForRead()
        val publish = ControlPacketV5.from(buffer) as PublishMessage<*>
        assertTrue(publish.variable.properties.payloadFormatIndicator)
    }

    @Test
    fun payloadFormatIndicatorFalse() {
        val props = VariableHeader.Properties<Any>(false)
        val buffer = allocateNewBuffer(6u, limits)
        val variableHeader = VariableHeader("t", properties = props)
        val expected = PublishMessage<Unit>(variable = variableHeader)
        expected.serialize(buffer)
        buffer.resetForRead()
        assertEquals(0b00110000, buffer.readByte(), "fixed header byte 1")
        assertEquals(4u, buffer.readVariableByteInteger(), "fixed header remaining length")
        assertEquals("t", buffer.readMqttUtf8StringNotValidated().toString(), "topic name")
        val propertiesActual = buffer.readProperties()
        assertEquals(0, propertiesActual?.count() ?: 0, "properties")
        assertNull((propertiesActual?.firstOrNull() as? PayloadFormatIndicator)?.willMessageIsUtf8)
        buffer.resetForRead()
        val publish = ControlPacketV5.from(buffer) as PublishMessage<*>
        assertFalse(publish.variable.properties.payloadFormatIndicator)
    }

    @Test
    fun payloadFormatIndicatorDuplicateThrowsProtocolError() {
        val obj1 = PayloadFormatIndicator(false)
        val obj2 = obj1.copy()
        val buffer = allocateNewBuffer(5u, limits)
        buffer.writeVariableByteInteger(obj1.size() + obj2.size())
        obj1.write(buffer)
        obj2.write(buffer)
        buffer.resetForRead()
        try {
            VariableHeader.Properties.from(buffer.readProperties())
            fail()
        } catch (e: ProtocolError) {
        }
    }

    @Test
    fun messageExpiryInterval() {
        val props = VariableHeader.Properties<Any>(messageExpiryInterval = 2)
        val variableHeader = VariableHeader("t", properties = props)
        val buffer = allocateNewBuffer(11u, limits)
        val msg = PublishMessage<Unit>(variable = variableHeader)
        msg.serialize(buffer)
        buffer.resetForRead()
        assertEquals(0b00110000, buffer.readByte(), "fixed header byte 1")
        assertEquals(9u, buffer.readVariableByteInteger(), "fixed header remaining length")
        assertEquals("t", buffer.readMqttUtf8StringNotValidated().toString(), "topic name")
        val propertiesActual = buffer.readProperties()
        assertEquals(1, propertiesActual?.count() ?: 0, "properties")
        assertEquals(
            props.messageExpiryInterval,
            (propertiesActual?.firstOrNull() as? MessageExpiryInterval)?.seconds
        )
        buffer.resetForRead()
        val publish = ControlPacketV5.from(buffer) as PublishMessage<*>
        assertEquals(2, publish.variable.properties.messageExpiryInterval)
    }

    @Test
    fun messageExpiryIntervalDuplicateThrowsProtocolError() {
        val obj1 = MessageExpiryInterval(2)
        val obj2 = obj1.copy()
        val buffer = allocateNewBuffer(11u, limits)
        buffer.writeVariableByteInteger(obj1.size() + obj2.size())
        obj1.write(buffer)
        obj2.write(buffer)
        buffer.resetForRead()
        try {
            VariableHeader.Properties.from(buffer.readProperties())
            fail()
        } catch (e: ProtocolError) {
        }
    }

    @Test
    fun topicAlias() {
        val props = VariableHeader.Properties<Any>(topicAlias = 2)
        val variableHeader = VariableHeader("t", properties = props)
        val expected = PublishMessage<Unit>(variable = variableHeader)
        val buffer = allocateNewBuffer(9u, limits)
        expected.serialize(buffer)
        buffer.resetForRead()
        assertEquals(0b00110000, buffer.readByte(), "fixed header byte 1")
        assertEquals(7u, buffer.readVariableByteInteger(), "fixed header remaining length")
        assertEquals("t", buffer.readMqttUtf8StringNotValidated().toString(), "topic name")
        val propertiesActual = buffer.readProperties()
        assertEquals(1, propertiesActual?.count() ?: 0, "properties")
        assertEquals(2, (propertiesActual?.firstOrNull() as? TopicAlias)?.value)
        buffer.resetForRead()
        val publish = ControlPacketV5.from(buffer) as PublishMessage<*>
        assertEquals(expected, publish)
    }


    @Test
    fun topicAliasZeroValueThrowsProtocolError() {
        try {
            VariableHeader.Properties<Unit>(topicAlias = 0)
            fail()
        } catch (e: ProtocolError) {
        }
        assertFails {
            PublishMessage<Unit>(
                variable = VariableHeader(
                    properties = VariableHeader.Properties(topicAlias = 0),
                    topicName = "yolo"
                )
            )
        }
    }

    @Test
    fun topicAliasDuplicateThrowsProtocolError() {
        val obj1 = TopicAlias(2)
        val obj2 = obj1.copy()
        val buffer = allocateNewBuffer(7u, limits)
        buffer.writeVariableByteInteger(obj1.size() + obj2.size())
        obj1.write(buffer)
        obj2.write(buffer)
        buffer.resetForRead()
        try {
            VariableHeader.Properties.from(buffer.readProperties())
            fail()
        } catch (e: ProtocolError) {
        }
    }

    @Test
    fun responseTopic() {
        val props = VariableHeader.Properties<Any>(responseTopic = "t/as")
        val variableHeader = VariableHeader("t", properties = props)
        val buffer = allocateNewBuffer(13u, limits)
        val actual = PublishMessage<Unit>(variable = variableHeader)
        actual.serialize(buffer)
        buffer.resetForRead()
        assertEquals(0b00110000, buffer.readByte(), "fixed header byte 1")
        assertEquals(11u, buffer.readVariableByteInteger(), "fixed header remaining length")
        assertEquals("t", buffer.readMqttUtf8StringNotValidated().toString(), "topic name")
        assertEquals(7u, buffer.readVariableByteInteger(), "property length")
        assertEquals(0x08, buffer.readByte(), "property identifier response topic")
        assertEquals("t/as", buffer.readMqttUtf8StringNotValidated().toString(), "response topic value")
        buffer.resetForRead()
        val publish = ControlPacketV5.from(buffer) as PublishMessage<*>
        assertEquals("t/as", publish.variable.properties.responseTopic?.toString())
    }

    @Test
    fun responseTopicDuplicateThrowsProtocolError() {
        val obj1 = ResponseTopic("t/as")
        val obj2 = obj1.copy()
        val buffer = allocateNewBuffer(15u, limits)
        buffer.writeVariableByteInteger(obj1.size() + obj2.size())
        obj1.write(buffer)
        obj2.write(buffer)
        buffer.resetForRead()
        try {
            VariableHeader.Properties.from(buffer.readProperties())
            fail()
        } catch (e: ProtocolError) {
        }
    }

    @Test
    fun correlationData() {
        val props = VariableHeader.Properties(correlationData = GenericType("yoyo", String::class))
        val variableHeader = VariableHeader("t", properties = props)
        val buffer = allocateNewBuffer(13u, limits)
        val actual = PublishMessage<Unit>(variable = variableHeader)
        actual.serialize(buffer)
        buffer.resetForRead()
        assertEquals(0b00110000, buffer.readByte(), "fixed header byte 1")
        assertEquals(11u, buffer.readVariableByteInteger(), "fixed header remaining length")
        assertEquals("t", buffer.readMqttUtf8StringNotValidated().toString(), "topic name")
        assertEquals(7u, buffer.readVariableByteInteger(), "property length")
        assertEquals(0x09, buffer.readByte(), "property identifier correlation data")
        assertEquals(4u, buffer.readUnsignedShort(), "property binary data size for correlation data")
        assertEquals("yoyo", buffer.readUtf8(4u).toString(), "correlation data payload")
        buffer.resetForRead()
        val publish = ControlPacketV5.from(buffer) as PublishMessage<*>
        assertEquals("yoyo", publish.variable.properties.correlationData?.obj?.toString())
    }

    @Test
    fun correlationDataDuplicateThrowsProtocolError() {
        val obj1 = CorrelationData(GenericType("yoyo", String::class))
        val obj2 = obj1.copy()
        val buffer = allocateNewBuffer(15u, limits)
        buffer.writeVariableByteInteger(obj1.size() + obj2.size())
        obj1.write(buffer)
        obj2.write(buffer)
        buffer.resetForRead()
        try {
            VariableHeader.Properties.from(buffer.readProperties())
            fail()
        } catch (e: ProtocolError) {
        }
    }

    @Test
    fun variableHeaderPropertyUserProperty() {
        val props = VariableHeader.Properties.from(setOf(UserProperty("key", "value")))
        val userPropertyResult = props.userProperty
        for ((key, value) in userPropertyResult) {
            assertEquals(key, "key")
            assertEquals(value, "value")
        }
        assertEquals(userPropertyResult.size, 1)

        val request = PublishMessage<Unit>(variable = VariableHeader("t", properties = props))
        val buffer = allocateNewBuffer(100u, limits)
        request.serialize(buffer)
        buffer.resetForRead()
        val requestRead = ControlPacketV5.from(buffer) as PublishMessage<*>
        val (key, value) = requestRead.variable.properties.userProperty.first()
        assertEquals("key", key.toString())
        assertEquals("value", value.toString())
    }

    @Test
    fun subscriptionIdentifier() {
        val props = VariableHeader.Properties<Unit>(subscriptionIdentifier = setOf(2))
        val variableHeader = VariableHeader("t", properties = props)
        val buffer = allocateNewBuffer(8u, limits)
        val actual = PublishMessage<Unit>(variable = variableHeader)
        actual.serialize(buffer)
        buffer.resetForRead()
        val publish = ControlPacketV5.from(buffer) as PublishMessage<*>
        assertEquals(2, publish.variable.properties.subscriptionIdentifier.first())
    }

    @Test
    fun subscriptionIdentifierZeroThrowsProtocolError() {
        val obj1 = SubscriptionIdentifier(0)
        val buffer = allocateNewBuffer(6u, limits)
        val size = obj1.size()
        buffer.writeVariableByteInteger(size)
        obj1.write(buffer)
        buffer.resetForRead()
        assertFailsWith<ProtocolError> { VariableHeader.Properties.from(buffer.readProperties()) }
    }

    @Test
    fun contentType() {
        val props = VariableHeader.Properties<Unit>(contentType = "t/as")
        val variableHeader = VariableHeader("t", properties = props)
        val buffer = allocateNewBuffer(13u, limits)
        val actual = PublishMessage<Unit>(variable = variableHeader)
        actual.serialize(buffer)
        buffer.resetForRead()
        val publish = ControlPacketV5.from(buffer) as PublishMessage<*>
        assertEquals("t/as", publish.variable.properties.contentType?.toString())
    }

    @Test
    fun contentTypeDuplicateThrowsProtocolError() {
        val obj1 = ContentType("t/as")
        val obj2 = obj1.copy()
        val buffer = allocateNewBuffer(15u, limits)
        buffer.writeVariableByteInteger(obj1.size() + obj2.size())
        obj1.write(buffer)
        obj2.write(buffer)
        buffer.resetForRead()
        assertFailsWith<ProtocolError> { VariableHeader.Properties.from(buffer.readProperties()) }
    }

    @Test
    fun invalidPropertyOnVariableHeaderThrowsMalformedPacketException() {
        val method = WillDelayInterval(3)
        try {
            VariableHeader.Properties.from(listOf(method, method))
            fail()
        } catch (e: MalformedPacketException) {
        }
    }

    @Test
    fun qos0AndPacketIdentifierThrowsIllegalArgumentException() {
        val fixed = FixedHeader(qos = QualityOfService.AT_MOST_ONCE)
        val variable = VariableHeader("t", 2)
        try {
            PublishMessage<Unit>(fixed, variable)
            fail()
        } catch (e: IllegalArgumentException) {
        }
    }

    @Test
    fun qos1WithoutPacketIdentifierThrowsIllegalArgumentException() {
        val fixed = FixedHeader(qos = QualityOfService.AT_LEAST_ONCE)
        val variable = VariableHeader("t")
        try {
            PublishMessage<Unit>(fixed, variable)
            fail()
        } catch (e: IllegalArgumentException) {
        }
    }

    @Test
    fun qos2WithoutPacketIdentifierThrowsIllegalArgumentException() {
        val fixed = FixedHeader(qos = QualityOfService.EXACTLY_ONCE)
        val variable = VariableHeader("t")
        try {
            PublishMessage<Unit>(fixed, variable)
            fail()
        } catch (e: IllegalArgumentException) {
        }
    }
}
