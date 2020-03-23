@file:Suppress("EXPERIMENTAL_API_USAGE")

package mqtt.wire5.control.packet

import kotlinx.io.core.buildPacket
import kotlinx.io.core.readBytes
import kotlinx.io.core.toByteArray
import kotlinx.io.core.writeFully
import mqtt.wire.MalformedPacketException
import mqtt.wire.ProtocolError
import mqtt.wire.data.ByteArrayWrapper
import mqtt.wire.data.MqttUtf8String
import mqtt.wire.data.QualityOfService
import mqtt.wire.data.VariableByteInteger
import mqtt.wire5.control.packet.PublishMessage.FixedHeader
import mqtt.wire5.control.packet.PublishMessage.VariableHeader
import mqtt.wire5.control.packet.format.variable.property.*
import kotlin.test.*

class PublishMessageTestsLegacy {

    @Test
    fun qosBothBitsSetTo1ThrowsMalformedPacketException() {
        val byte1 = 0b00111110.toByte()
        val remainingLength = 1.toByte()
        val packet = buildPacket {
            writeByte(byte1)
            writeByte(remainingLength)
        }
        try {
            ControlPacketV5.from(packet)
            fail()
        } catch (e: MalformedPacketException) {
        }
    }

    @Test
    fun payloadFormatIndicatorDefault() {
        val props = VariableHeader.Properties()
        val variableHeader = VariableHeader("t", properties = props)
        val publishByteReadPacket = PublishMessage(variable = variableHeader).serialize()
        val publish = ControlPacketV5.from(publishByteReadPacket) as PublishMessage
        assertFalse(publish.variable.properties.payloadFormatIndicator)
    }

    @Test
    fun payloadFormatIndicatorTrue() {
        val props = VariableHeader.Properties(true)
        val variableHeader = VariableHeader("t", properties = props)
        val publishByteReadPacket = PublishMessage(variable = variableHeader).serialize()
        val publish = ControlPacketV5.from(publishByteReadPacket) as PublishMessage
        assertTrue(publish.variable.properties.payloadFormatIndicator)
    }

    @Test
    fun payloadFormatIndicatorFalse() {
        val props = VariableHeader.Properties(false)
        val variableHeader = VariableHeader("t", properties = props)
        val publishByteReadPacket = PublishMessage(variable = variableHeader).serialize()
        val publish = ControlPacketV5.from(publishByteReadPacket) as PublishMessage
        assertFalse(publish.variable.properties.payloadFormatIndicator)
    }

    @Test
    fun payloadFormatIndicatorDuplicateThrowsProtocolError() {
        val obj1 = PayloadFormatIndicator(false)
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
    fun messageExpiryInterval() {
        val props = VariableHeader.Properties(messageExpiryInterval = 2)
        val variableHeader = VariableHeader("t", properties = props)
        val publishByteReadPacket = PublishMessage(variable = variableHeader).serialize()
        val publish = ControlPacketV5.from(publishByteReadPacket) as PublishMessage
        assertEquals(2, publish.variable.properties.messageExpiryInterval)
    }

    @Test
    fun messageExpiryIntervalDuplicateThrowsProtocolError() {
        val obj1 = MessageExpiryInterval(2)
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
    fun topicAlias() {
        val props = VariableHeader.Properties(topicAlias = 2)
        val variableHeader = VariableHeader("t", properties = props)
        val publishByteReadPacket = PublishMessage(variable = variableHeader).serialize()
        val publish = ControlPacketV5.from(publishByteReadPacket) as PublishMessage
        assertEquals(2, publish.variable.properties.topicAlias)
    }


    @Test
    fun topicAliasZeroValueThrowsProtocolError() {
        try {
            VariableHeader.Properties(topicAlias = 0)
            fail()
        } catch (e: ProtocolError) {
        }
        val obj1 = TopicAlias(0)
        val propsWithoutPropertyLength = buildPacket {
            obj1.write(this)
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
    fun topicAliasDuplicateThrowsProtocolError() {
        val obj1 = TopicAlias(2)
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
    fun responseTopic() {
        val props = VariableHeader.Properties(responseTopic = MqttUtf8String("t/as"))
        val variableHeader = VariableHeader("t", properties = props)
        val publishByteReadPacket = PublishMessage(variable = variableHeader).serialize()
        val publish = ControlPacketV5.from(publishByteReadPacket) as PublishMessage
        assertEquals("t/as", publish.variable.properties.responseTopic?.getValueOrThrow())
    }

    @Test
    fun responseTopicDuplicateThrowsProtocolError() {
        val obj1 = ResponseTopic(MqttUtf8String("t/as"))
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
    fun correlationData() {
        val props = VariableHeader.Properties(coorelationData = ByteArrayWrapper("t/as".toByteArray()))
        val variableHeader = VariableHeader("t", properties = props)
        val publishByteReadPacket = PublishMessage(variable = variableHeader).serialize()
        val publish = ControlPacketV5.from(publishByteReadPacket) as PublishMessage
        assertEquals(ByteArrayWrapper("t/as".toByteArray()), publish.variable.properties.coorelationData)
    }

    @Test
    fun correlationDataDuplicateThrowsProtocolError() {
        val obj1 = CorrelationData(ByteArrayWrapper("t/as".toByteArray()))
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

        val request = PublishMessage(variable = VariableHeader("t", properties = props)).serialize()
        val requestRead = ControlPacketV5.from(request.copy()) as PublishMessage
        val (key, value) = requestRead.variable.properties.userProperty.first()
        assertEquals(key.getValueOrThrow(), "key")
        assertEquals(value.getValueOrThrow(), "value")
    }

    @Test
    fun subscriptionIdentifier() {
        val props = VariableHeader.Properties(subscriptionIdentifier = setOf(2))
        val variableHeader = VariableHeader("t", properties = props)
        val publishByteReadPacket = PublishMessage(variable = variableHeader).serialize()
        val publish = ControlPacketV5.from(publishByteReadPacket) as PublishMessage
        assertEquals(2, publish.variable.properties.subscriptionIdentifier.first())
    }

    @Test
    fun subscriptionIdentifierZeroThrowsProtocolError() {
        val obj1 = SubscriptionIdentifier(0)
        val propsWithoutPropertyLength = buildPacket {
            obj1.write(this)
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
    fun contentType() {
        val props = VariableHeader.Properties(contentType = MqttUtf8String("t/as"))
        val variableHeader = VariableHeader("t", properties = props)
        val publishByteReadPacket = PublishMessage(variable = variableHeader).serialize()
        val publish = ControlPacketV5.from(publishByteReadPacket) as PublishMessage
        assertEquals("t/as", publish.variable.properties.contentType?.getValueOrThrow())
    }

    @Test
    fun contentTypeDuplicateThrowsProtocolError() {
        val obj1 = ContentType(MqttUtf8String("t/as"))
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
            PublishMessage(fixed, variable)
            fail()
        } catch (e: IllegalArgumentException) {
        }
    }

    @Test
    fun qos1WithoutPacketIdentifierThrowsIllegalArgumentException() {
        val fixed = FixedHeader(qos = QualityOfService.AT_LEAST_ONCE)
        val variable = VariableHeader("t")
        try {
            PublishMessage(fixed, variable)
            fail()
        } catch (e: IllegalArgumentException) {
        }
    }

    @Test
    fun qos2WithoutPacketIdentifierThrowsIllegalArgumentException() {
        val fixed = FixedHeader(qos = QualityOfService.EXACTLY_ONCE)
        val variable = VariableHeader("t")
        try {
            PublishMessage(fixed, variable)
            fail()
        } catch (e: IllegalArgumentException) {
        }
    }
}
