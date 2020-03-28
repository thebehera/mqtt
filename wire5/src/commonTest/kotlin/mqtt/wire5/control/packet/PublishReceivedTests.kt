@file:Suppress("EXPERIMENTAL_API_USAGE", "EXPERIMENTAL_UNSIGNED_LITERALS")

package mqtt.wire5.control.packet

import mqtt.buffer.allocateNewBuffer
import mqtt.wire.ProtocolError
import mqtt.wire.control.packet.format.ReasonCode.*
import mqtt.wire.data.MqttUtf8String
import mqtt.wire5.control.packet.PublishReceived.VariableHeader
import mqtt.wire5.control.packet.format.variable.property.ReasonString
import mqtt.wire5.control.packet.format.variable.property.UserProperty
import mqtt.wire5.control.packet.format.variable.property.readProperties
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.fail

class PublishReceivedTests {
    private val packetIdentifier = 2

    @Test
    fun packetIdentifier() {
        val pubrec = PublishReceived(VariableHeader(packetIdentifier))
        val buffer = allocateNewBuffer(4u, limits)
        pubrec.serialize(buffer)
        buffer.resetForRead()
        assertEquals(0b01010000, buffer.readByte(), "fixed header byte1")
        assertEquals(2u, buffer.readVariableByteInteger(), "fixed header byte2 remaining length")
        assertEquals(packetIdentifier, buffer.readUnsignedShort().toInt(), "variable header byte 1-2")
        buffer.resetForRead()
        val pubrecResult = ControlPacketV5.from(buffer) as PublishReceived
        assertEquals(pubrecResult.variable.packetIdentifier, packetIdentifier)
    }

    @Test
    fun defaultAndNonDefaultSuccessDeserialization() {
        val pubrec = PublishReceived(VariableHeader(packetIdentifier))
        val bufferNonDefaults = allocateNewBuffer(6u, limits)
        bufferNonDefaults.write(0b01010000)
        bufferNonDefaults.writeVariableByteInteger(4u)
        bufferNonDefaults.write(packetIdentifier.toUShort())
        bufferNonDefaults.write(0.toUByte())
        bufferNonDefaults.writeVariableByteInteger(0u)
        bufferNonDefaults.resetForRead()
        val pubrecResult = ControlPacketV5.from(bufferNonDefaults) as PublishReceived
        assertEquals(pubrec, pubrecResult)

    }

    @Test
    fun noMatchingSubscribers() {
        val pubrec = PublishReceived(VariableHeader(packetIdentifier, NO_MATCHING_SUBSCRIBERS))
        val buffer = allocateNewBuffer(6u, limits)
        pubrec.serialize(buffer)
        buffer.resetForRead()
        assertEquals(0b01010000, buffer.readByte(), "fixed header byte1")
        assertEquals(4u, buffer.readVariableByteInteger(), "fixed header byte2 remaining length")
        assertEquals(packetIdentifier, buffer.readUnsignedShort().toInt(), "variable header byte 1-2")
        assertEquals(NO_MATCHING_SUBSCRIBERS.byte, buffer.readUnsignedByte(), "reason code")
        assertEquals(0u, buffer.readVariableByteInteger(), "property length")
        buffer.resetForRead()
        val pubrecResult = ControlPacketV5.from(buffer) as PublishReceived
        assertEquals(pubrecResult.variable.reasonCode, NO_MATCHING_SUBSCRIBERS)
    }

    @Test
    fun unspecifiedError() {
        val pubrec = PublishReceived(VariableHeader(packetIdentifier, UNSPECIFIED_ERROR))
        val buffer = allocateNewBuffer(6u, limits)
        pubrec.serialize(buffer)
        buffer.resetForRead()
        assertEquals(0b01010000, buffer.readByte(), "fixed header byte1")
        assertEquals(4u, buffer.readVariableByteInteger(), "fixed header byte2 remaining length")
        assertEquals(packetIdentifier, buffer.readUnsignedShort().toInt(), "variable header byte 1-2")
        assertEquals(UNSPECIFIED_ERROR.byte, buffer.readUnsignedByte(), "reason code")
        assertEquals(0u, buffer.readVariableByteInteger(), "property length")
        buffer.resetForRead()
        val pubrecResult = ControlPacketV5.from(buffer) as PublishReceived
        assertEquals(pubrecResult.variable.reasonCode, UNSPECIFIED_ERROR)
    }

    @Test
    fun implementationSpecificError() {
        val pubrec = PublishReceived(VariableHeader(packetIdentifier, IMPLEMENTATION_SPECIFIC_ERROR))
        val buffer = allocateNewBuffer(6u, limits)
        pubrec.serialize(buffer)
        buffer.resetForRead()
        assertEquals(0b01010000, buffer.readByte(), "fixed header byte1")
        assertEquals(4u, buffer.readVariableByteInteger(), "fixed header byte2 remaining length")
        assertEquals(packetIdentifier, buffer.readUnsignedShort().toInt(), "variable header byte 1-2")
        assertEquals(IMPLEMENTATION_SPECIFIC_ERROR.byte, buffer.readUnsignedByte(), "reason code")
        assertEquals(0u, buffer.readVariableByteInteger(), "property length")
        buffer.resetForRead()
        val pubrecResult = ControlPacketV5.from(buffer) as PublishReceived
        assertEquals(pubrecResult.variable.reasonCode, IMPLEMENTATION_SPECIFIC_ERROR)
    }

    @Test
    fun notAuthorized() {
        val pubrec = PublishReceived(VariableHeader(packetIdentifier, NOT_AUTHORIZED))
        val buffer = allocateNewBuffer(6u, limits)
        pubrec.serialize(buffer)
        buffer.resetForRead()
        assertEquals(0b01010000, buffer.readByte(), "fixed header byte1")
        assertEquals(4u, buffer.readVariableByteInteger(), "fixed header byte2 remaining length")
        assertEquals(packetIdentifier, buffer.readUnsignedShort().toInt(), "variable header byte 1-2")
        assertEquals(NOT_AUTHORIZED.byte, buffer.readUnsignedByte(), "reason code")
        assertEquals(0u, buffer.readVariableByteInteger(), "property length")
        buffer.resetForRead()
        val pubrecResult = ControlPacketV5.from(buffer) as PublishReceived
        assertEquals(pubrecResult.variable.reasonCode, NOT_AUTHORIZED)
    }

    @Test
    fun topicNameInvalid() {
        val pubrec = PublishReceived(VariableHeader(packetIdentifier, TOPIC_NAME_INVALID))
        val buffer = allocateNewBuffer(6u, limits)
        pubrec.serialize(buffer)
        buffer.resetForRead()
        assertEquals(0b01010000, buffer.readByte(), "fixed header byte1")
        assertEquals(4u, buffer.readVariableByteInteger(), "fixed header byte2 remaining length")
        assertEquals(packetIdentifier, buffer.readUnsignedShort().toInt(), "variable header byte 1-2")
        assertEquals(TOPIC_NAME_INVALID.byte, buffer.readUnsignedByte(), "reason code")
        assertEquals(0u, buffer.readVariableByteInteger(), "property length")
        buffer.resetForRead()
        val pubrecResult = ControlPacketV5.from(buffer) as PublishReceived
        assertEquals(pubrecResult.variable.reasonCode, TOPIC_NAME_INVALID)
    }

    @Test
    fun packetIdentifierInUse() {
        val pubrec = PublishReceived(VariableHeader(packetIdentifier, PACKET_IDENTIFIER_IN_USE))
        val buffer = allocateNewBuffer(6u, limits)
        pubrec.serialize(buffer)
        buffer.resetForRead()
        assertEquals(0b01010000, buffer.readByte(), "fixed header byte1")
        assertEquals(4u, buffer.readVariableByteInteger(), "fixed header byte2 remaining length")
        assertEquals(packetIdentifier, buffer.readUnsignedShort().toInt(), "variable header byte 1-2")
        assertEquals(PACKET_IDENTIFIER_IN_USE.byte, buffer.readUnsignedByte(), "reason code")
        assertEquals(0u, buffer.readVariableByteInteger(), "property length")
        buffer.resetForRead()
        val pubrecResult = ControlPacketV5.from(buffer) as PublishReceived
        assertEquals(pubrecResult.variable.reasonCode, PACKET_IDENTIFIER_IN_USE)
    }

    @Test
    fun quotaExceeded() {
        val pubrec = PublishReceived(VariableHeader(packetIdentifier, QUOTA_EXCEEDED))
        val buffer = allocateNewBuffer(6u, limits)
        pubrec.serialize(buffer)
        buffer.resetForRead()
        assertEquals(0b01010000, buffer.readByte(), "fixed header byte1")
        assertEquals(4u, buffer.readVariableByteInteger(), "fixed header byte2 remaining length")
        assertEquals(packetIdentifier, buffer.readUnsignedShort().toInt(), "variable header byte 1-2")
        assertEquals(QUOTA_EXCEEDED.byte, buffer.readUnsignedByte(), "reason code")
        assertEquals(0u, buffer.readVariableByteInteger(), "property length")
        buffer.resetForRead()
        val pubrecResult = ControlPacketV5.from(buffer) as PublishReceived
        assertEquals(pubrecResult.variable.reasonCode, QUOTA_EXCEEDED)
    }

    @Test
    fun payloadFormatInvalid() {
        val pubrec = PublishReceived(VariableHeader(packetIdentifier, PAYLOAD_FORMAT_INVALID))
        val buffer = allocateNewBuffer(6u, limits)
        pubrec.serialize(buffer)
        buffer.resetForRead()
        assertEquals(0b01010000, buffer.readByte(), "fixed header byte1")
        assertEquals(4u, buffer.readVariableByteInteger(), "fixed header byte2 remaining length")
        assertEquals(packetIdentifier, buffer.readUnsignedShort().toInt(), "variable header byte 1-2")
        assertEquals(PAYLOAD_FORMAT_INVALID.byte, buffer.readUnsignedByte(), "reason code")
        assertEquals(0u, buffer.readVariableByteInteger(), "property length")
        buffer.resetForRead()
        val pubrecResult = ControlPacketV5.from(buffer) as PublishReceived
        assertEquals(pubrecResult.variable.reasonCode, PAYLOAD_FORMAT_INVALID)
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
        val expected = PublishReceived(VariableHeader(packetIdentifier, properties = VariableHeader.Properties(reasonString = MqttUtf8String("yolo"))))
        val buffer = allocateNewBuffer(13u, limits)
        expected.serialize(buffer)
        buffer.resetForRead()
        assertEquals(0b01010000, buffer.readByte(), "fixed header byte1")
        assertEquals(11u, buffer.readVariableByteInteger(), "fixed header byte2 remaining length")
        assertEquals(packetIdentifier, buffer.readUnsignedShort().toInt(), "variable header byte 1-2")
        assertEquals(SUCCESS.byte, buffer.readUnsignedByte(), "reason code")
        assertEquals(7u, buffer.readVariableByteInteger(), "property length")
        assertEquals(0x1F, buffer.readByte(), "user property identifier")
        assertEquals("yolo", buffer.readMqttUtf8StringNotValidated().toString(), "reason string")
        buffer.resetForRead()
        val pubrecResult = ControlPacketV5.from(buffer) as PublishReceived
        assertEquals(expected.variable.properties.reasonString, MqttUtf8String("yolo"))
        assertEquals(expected, pubrecResult)
    }

    @Test
    fun reasonStringMultipleTimesThrowsProtocolError() {
        val obj1 = ReasonString(MqttUtf8String("yolo"))
        val obj2 = obj1.copy()
        val buffer = allocateNewBuffer(15u, limits)
        buffer.writeVariableByteInteger(obj1.size(buffer) + obj2.size(buffer))
        obj1.write(buffer)
        obj2.write(buffer)
        buffer.resetForRead()
        assertFailsWith<ProtocolError> { VariableHeader.Properties.from(buffer.readProperties()) }
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

        val buffer = allocateNewBuffer(19u, limits)
        val request = PublishReceived(VariableHeader(packetIdentifier, properties = props))
        request.serialize(buffer)
        buffer.resetForRead()
        val requestRead = ControlPacketV5.from(buffer) as PublishReceived
        val (key, value) = requestRead.variable.properties.userProperty.first()
        assertEquals(key.getValueOrThrow().toString(), "key")
        assertEquals(value.getValueOrThrow().toString(), "value")
    }

}
