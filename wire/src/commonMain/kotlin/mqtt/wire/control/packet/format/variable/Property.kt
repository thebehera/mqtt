@file:Suppress("EXPERIMENTAL_API_USAGE")

package mqtt.wire.control.packet.format.variable

import kotlinx.io.core.ByteReadPacket
import kotlinx.io.core.readUInt
import kotlinx.io.core.readUShort
import mqtt.wire.data.*
import mqtt.wire.data.Type.*

enum class Property(val identifierByte: Byte, val type: Type, val willProperties: Boolean = false) {
    PAYLOAD_FORMAT_INDICATOR(0x01, BYTE, willProperties = true),
    MESSAGE_EXPIRY_INTERVAL(0x02, FOUR_BYTE_INTEGER, willProperties = true),
    CONTENT_TYPE(0x03, UTF_8_ENCODED_STRING, willProperties = true),
    RESPONSE_TOPIC(0x08, UTF_8_ENCODED_STRING, willProperties = true),
    CORRELATION_DATA(0x09, BINARY_DATA, willProperties = true),
    SUBSCRIPTION_IDENTIFIER(0x0B, VARIABLE_BYTE_INTEGER),
    SESSION_EXPIRY_INTERVAL(0x11, FOUR_BYTE_INTEGER),
    ASSIGNED_CLIENT_IDENTIFIER(0x12, UTF_8_ENCODED_STRING),
    SERVER_KEEP_ALIVE(0x13, TWO_BYTE_INTEGER),
    AUTHENTICATION_METHOD(0x15, UTF_8_ENCODED_STRING),
    AUTHENTICATION_DATA(0x16, BINARY_DATA),
    REQUEST_PROBLEM_INFORMATION(0x17, BYTE),
    WILL_DELAY_INTERVAL(0x18, FOUR_BYTE_INTEGER, willProperties = true),
    REQUEST_RESPONSE_INFORMATION(0x19, BYTE),
    RESPONSE_INFORMATION(0x1A, UTF_8_ENCODED_STRING),
    SERVER_REFERENCE(0x1C, UTF_8_ENCODED_STRING),
    REASON_STRING(0x1F, UTF_8_ENCODED_STRING),
    RECEIVE_MAXIMUM(0x21, TWO_BYTE_INTEGER),
    TOPIC_ALIAS_MAXIMUM(0x22, TWO_BYTE_INTEGER),
    TOPIC_ALIAS(0x22, TWO_BYTE_INTEGER),
    MAXIMUM_QOS(0x24, BYTE),
    RETAIN_AVAILABLE(0x25, BYTE),
    USER_PROPERTY(0x26, UTF_8_ENCODED_STRING, willProperties = true),
    MAXIMUM_PACKET_SIZE(0x27, FOUR_BYTE_INTEGER),
    WILDCARD_SUBSCRIPTION_AVAILABLE(0x28, BYTE),
    SUBSCRIPTION_IDENTIFIER_AVAILABLE(0x29, BYTE),
    SHARED_SUBSCRIPTION_AVAILABLE(0x2A, BYTE);

    val identifier get() = VariableByteInteger(identifierByte.toUInt())
}
fun ByteReadPacket.readProperties() :Collection<PropertyKeyValueWrapper> {
    val propertyLength = decodeVariableByteInteger().toInt()
    val list = mutableListOf<PropertyKeyValueWrapper>()
    var propertyIndex = 0
    do {
        val propertyIdentifier = readByte()
        propertyIndex++
        val property = propertyMap[propertyIdentifier] ?: continue
        val byteStartIndex = remaining
        list += when (property.type) {
            BYTE -> PropertyKeyValueWrapper(property, number = readByte().toUInt(), type = property.type)
            TWO_BYTE_INTEGER -> PropertyKeyValueWrapper(property, number = readUShort().toUInt(), type = property.type)
            FOUR_BYTE_INTEGER -> PropertyKeyValueWrapper(property, number = readUInt(), type = property.type)
            UTF_8_ENCODED_STRING -> {
                val value = readMqttUtf8String().getValueOrThrow()
                propertyIndex += UShort.SIZE_BYTES + value.length
                PropertyKeyValueWrapper(property, value = value, type = property.type)
            }
            BINARY_DATA -> PropertyKeyValueWrapper(property, binary = readMqttBinary(), type = property.type)
            VARIABLE_BYTE_INTEGER -> PropertyKeyValueWrapper(property, number = decodeVariableByteInteger(), type = property.type)
            UTF_8_STRING_PAIR -> {
                val key = readMqttUtf8String().getValueOrThrow()
                propertyIndex += UShort.SIZE_BYTES + key.length
                val value = readMqttUtf8String().getValueOrThrow()
                propertyIndex += UShort.SIZE_BYTES + value.length
                PropertyKeyValueWrapper(property, key, value, type = property.type)
            }
        }
        val delta = byteStartIndex - remaining
        propertyIndex += delta.toInt()

    } while (propertyIndex < propertyLength)
    return list
}
internal val propertyMap by lazy { Property.values().map { it.identifierByte to it }.toMap() }


data class PropertyKeyValueWrapper(val property: Property, val key: String? = null, val value: String? = null, val number: UInt? = null, val binary:ByteArray? = null, val type: Type) {

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null || this::class != other::class) return false

        other as PropertyKeyValueWrapper

        if (property != other.property) return false
        if (key != other.key) return false
        if (value != other.value) return false
        if (number != other.number) return false
        if (binary != null) {
            if (other.binary == null) return false
            if (!binary.contentEquals(other.binary)) return false
        } else if (other.binary != null) return false

        return true
    }

    override fun hashCode(): Int {
        var result = property.hashCode()
        result = 31 * result + (key?.hashCode() ?: 0)
        result = 31 * result + (value?.hashCode() ?: 0)
        result = 31 * result + (number?.hashCode() ?: 0)
        result = 31 * result + (binary?.contentHashCode() ?: 0)
        return result
    }
}
