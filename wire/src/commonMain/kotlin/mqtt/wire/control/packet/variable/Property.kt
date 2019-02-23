@file:Suppress("EXPERIMENTAL_API_USAGE")

package mqtt.wire.control.packet.variable

import kotlinx.io.core.*
import mqtt.wire.control.packet.fixed.ControlPacketType
import mqtt.wire.control.packet.fixed.ControlPacketType.*
import mqtt.wire.data.Type
import mqtt.wire.data.Type.*
import mqtt.wire.data.encodeVariableByteInteger
import mqtt.wire.data.validateMqttUTF8String

enum class Property(val identifier: Byte, val type: Type, vararg val packetTypes: ControlPacketType, val willProperties: Boolean = false) {
    PAYLOAD_FORMAT_INDICATOR(0x01, BYTE, PUBLISH, willProperties = true),
    MESSAGE_EXPIRY_INTERVAL(0x02, FOUR_BYTE_INTEGER, PUBLISH, willProperties = true),
    CONTENT_TYPE(0x03, UTF_8_ENCODED_STRING, PUBLISH, willProperties = true),
    RESPONSE_TOPIC(0x08, UTF_8_ENCODED_STRING, PUBLISH, willProperties = true),
    CORRELATION_DATA(0x09, BINARY_DATA, PUBLISH, willProperties = true),
    SUBSCRIPTION_IDENTIFIER(0x0B, VARIABLE_BYTE_INTEGER, PUBLISH, SUBSCRIBE),
    SESSION_EXPIRY_INTERVAL(0x11, FOUR_BYTE_INTEGER, CONNECT, CONNACK, DISCONNECT),
    ASSIGNED_CLIENT_IDENTIFIER(0x12, UTF_8_ENCODED_STRING, CONNACK),
    SERVER_KEEP_ALIVE(0x13, TWO_BYTE_INTEGER, CONNACK),
    AUTHENTICATION_METHOD(0x15, UTF_8_ENCODED_STRING, CONNECT, CONNACK, AUTH),
    AUTHENTICATION_DATA(0x16, BINARY_DATA, CONNECT, CONNACK, AUTH),
    REQUEST_PROBLEM_INFORMATION(0x17, BYTE, CONNECT),
    WILL_DELAY_INTERVAL(0x18, FOUR_BYTE_INTEGER, willProperties = true),
    REQUEST_RESPONSE_INFORMATION(0x19, BYTE, CONNECT),
    RESPONSE_INFORMATION(0x1A, UTF_8_ENCODED_STRING, CONNACK),
    SERVER_REFERENCE(0x1C, UTF_8_ENCODED_STRING, CONNACK, DISCONNECT),
    REASON_STRING(0x1F, UTF_8_ENCODED_STRING, CONNACK, PUBACK, PUBREC, PUBREL, PUBCOMP, SUBACK, UNSUBACK, DISCONNECT, AUTH),
    RECEIVE_MAXIMUM(0x21, TWO_BYTE_INTEGER, CONNECT, CONNACK),
    TOPIC_ALIAS_MAXIMUM(0x22, TWO_BYTE_INTEGER, CONNECT, CONNACK),
    TOPIC_ALIAS(0x22, TWO_BYTE_INTEGER, PUBLISH),
    MAXIMUM_QOS(0x24, BYTE, CONNACK),
    RETAIN_AVAILABLE(0x25, BYTE, CONNACK),
    USER_PROPERTY(0x26, UTF_8_ENCODED_STRING, CONNECT, CONNACK, PUBLISH, PUBACK, PUBREC, PUBREL, PUBCOMP, SUBSCRIBE, SUBACK, UNSUBSCRIBE, UNSUBACK, DISCONNECT, AUTH, willProperties = true),
    MAXIMUM_PACKET_SIZE(0x27, FOUR_BYTE_INTEGER, CONNECT, CONNACK),
    WILDCARD_SUBSCRIPTION_AVAILABLE(0x28, BYTE, CONNACK),
    SUBSCRIPTION_IDENTIFIER_AVAILABLE(0x29, BYTE, CONNACK),
    SHARED_SUBSCRIPTION_AVAILABLE(0x2A, BYTE, CONNACK);

    fun build(byte: Byte): ByteArray {
        if (type != BYTE) {
            throw IllegalStateException("Calling wrong build function. Use $type instead.")
        }
        val identifierSizeBytes = Byte.SIZE_BYTES
        val byteSizeInBytes = Byte.SIZE_BYTES // Just to make it very clear
        val propertyLength = identifierSizeBytes + byteSizeInBytes
        return byteArrayOf(propertyLength.toByte(), identifier, byte)
    }

    fun build(twoByteInteger: UShort): ByteArray {
        if (type != TWO_BYTE_INTEGER) {
            throw IllegalStateException("Calling wrong build function. Use $type instead.")
        }
        val packet = buildPacket {
            val identifierSizeBytes = Byte.SIZE_BYTES
            val uIntSizeInBytes = UInt.SIZE_BYTES
            val propertyLength = identifierSizeBytes + uIntSizeInBytes
            writeByte(propertyLength.toByte())
            writeByte(identifier)
            writeUShort(twoByteInteger)
        }
        return packet.readBytes()
    }

    fun build(fourByteInteger: UInt): ByteArray {
        if (type != FOUR_BYTE_INTEGER) {
            throw IllegalStateException("Calling wrong build function. Use $type instead.")
        }
        val packet = buildPacket {
            val identifierSizeBytes = Byte.SIZE_BYTES
            val uIntSizeInBytes = UInt.SIZE_BYTES
            val propertyLength = identifierSizeBytes + uIntSizeInBytes
            writeByte(propertyLength.toByte())
            writeByte(identifier)
            writeUInt(fourByteInteger)
        }
        return packet.readBytes()
    }

    fun build(variableByteInteger: Int): ByteArray {
        if (type != VARIABLE_BYTE_INTEGER) {
            throw IllegalStateException("Calling wrong build function. Use $type instead.")
        }
        val packet = buildPacket {
            val identifierSizeBytes = Byte.SIZE_BYTES
            val data = variableByteInteger.encodeVariableByteInteger()
            val propertyLength = identifierSizeBytes + data.size
            writeByte(propertyLength.toByte())
            writeByte(identifier)
            writeFully(data)
        }
        return packet.readBytes()
    }

    fun build(string: String): ByteArray {
        if (type != UTF_8_ENCODED_STRING) {
            throw IllegalStateException("Calling wrong build function. Use $type instead.")
        }
        if (!string.validateMqttUTF8String()) {
            throw IllegalStateException("UTF-8 String does not pass MQTT validation")
        }
        val packet = buildPacket {
            val identifierSizeBytes = Byte.SIZE_BYTES
            val stringSizeInBytes = string.toByteArray().size
            val propertyLength = identifierSizeBytes + stringSizeInBytes
            writeByte(propertyLength.toByte())
            writeByte(identifier)
            writeStringUtf8(string)
        }
        return packet.readBytes()
    }

    fun build(data: ByteArray): ByteArray {
        if (type != BINARY_DATA) {
            throw IllegalStateException("Calling wrong build function. Use $type instead.")
        }
        val packet = buildPacket {
            val identifierSizeBytes = Byte.SIZE_BYTES
            val sizeInBytes = data.size
            val propertyLength = identifierSizeBytes + sizeInBytes
            writeByte(propertyLength.toByte())
            writeByte(identifier)
            writeFully(data)
        }
        return packet.readBytes()
    }
}

internal val propertyMap by lazy { Property.values().map { it.identifier to it }.toMap() }

