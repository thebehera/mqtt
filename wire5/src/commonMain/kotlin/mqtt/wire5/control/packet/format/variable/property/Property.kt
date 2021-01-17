@file:Suppress("EXPERIMENTAL_API_USAGE", "EXPERIMENTAL_UNSIGNED_LITERALS", "UNUSED_PARAMETER")

package mqtt.wire5.control.packet.format.variable.property

import mqtt.buffer.ReadBuffer
import mqtt.buffer.WriteBuffer
import mqtt.wire.MalformedPacketException
import mqtt.wire.ProtocolError
import mqtt.wire.buffer.GenericType
import mqtt.wire.buffer.readMqttUtf8StringNotValidated
import mqtt.wire.buffer.readVariableByteInteger
import mqtt.wire.buffer.writeMqttUtf8String
import mqtt.wire.data.QualityOfService
import mqtt.wire.data.Type
import mqtt.wire.data.utf8Length


abstract class Property(val identifierByte: Byte, val type: Type, val willProperties: Boolean = false) {
    open fun write(buffer: WriteBuffer): UInt {
        return 0u
    }

    open fun size(): UInt {
        return 0u
    }

    internal fun write(buffer: WriteBuffer, boolean: Boolean): UInt {
        buffer.write(identifierByte)
        buffer.write((if (boolean) 1 else 0).toUByte())
        return 2u
    }

    fun size(number: UInt) = 5u
    fun write(bytePacketBuilder: WriteBuffer, number: UInt): UInt {
        bytePacketBuilder.write(identifierByte)
        bytePacketBuilder.write(number)
        return 5u
    }

    fun size(number: UShort) = 3u
    fun write(bytePacketBuilder: WriteBuffer, number: UShort): UInt {
        bytePacketBuilder.write(identifierByte)
        bytePacketBuilder.write(number)
        return 3u
    }

    fun size(string: CharSequence) =
        string.utf8Length().toUInt() + UShort.SIZE_BYTES.toUInt() + 1u

    fun write(bytePacketBuilder: WriteBuffer, string: CharSequence): UInt {
        bytePacketBuilder.write(identifierByte)
        val size = string.utf8Length().toUInt()
        bytePacketBuilder.writeMqttUtf8String(string)
        return size
    }
}

fun Collection<Property?>.addTo(map: HashMap<Int, Any>) {
    forEach {
        map.addProperty(it)
    }
}

fun HashMap<Int, Any>.addProperty(property: Property?) {
    property ?: return
    put(property.identifierByte.toInt(), property)
}

fun ReadBuffer.readMqttProperty(): Pair<Property, Long> {
    val identifierByte = readByte().toInt()
    val property = when (identifierByte) {
        0x01 -> {
            PayloadFormatIndicator(readByte() == 1.toByte())
        }
        0x02 -> {
            MessageExpiryInterval(readUnsignedInt().toLong())
        }
        0x03 -> {
            ContentType(readMqttUtf8StringNotValidated())
        }
        0x08 -> ResponseTopic(readMqttUtf8StringNotValidated())
        0x09 -> CorrelationData(GenericType(readUtf8(readUnsignedShort().toUInt()), CharSequence::class))
        0x0B -> SubscriptionIdentifier(readVariableByteInteger().toLong())
        0x11 -> SessionExpiryInterval(readUnsignedInt().toLong())
        0x12 -> AssignedClientIdentifier(readMqttUtf8StringNotValidated())
        0x13 -> ServerKeepAlive(readUnsignedShort().toInt())
        0x15 -> AuthenticationMethod(readMqttUtf8StringNotValidated())
        0x16 -> AuthenticationData(GenericType(readUtf8(readUnsignedShort().toUInt()), CharSequence::class))
        0x17 -> {
            val uByteAsInt = readByte().toInt()
            if (!(uByteAsInt == 0 || uByteAsInt == 1)) {
                throw ProtocolError(
                    "Request Problem Information cannot have a value other than 0 or 1" +
                            "see: https://docs.oasis-open.org/mqtt/mqtt/v5.0/cos02/mqtt-v5.0-cos02.html#_Toc1477353"
                )
            }
            RequestProblemInformation(uByteAsInt == 1)
        }
        0x18 -> WillDelayInterval(readUnsignedInt().toLong())
        0x19 -> {
            val uByteAsInt = readUnsignedByte().toInt()
            if (!(uByteAsInt == 0 || uByteAsInt == 1)) {
                throw ProtocolError(
                    "Request Response Information cannot have a value other than 0 or 1" +
                            "see: https://docs.oasis-open.org/mqtt/mqtt/v5.0/cos02/mqtt-v5.0-cos02.html#_Toc1477352"
                )
            }
            RequestResponseInformation(uByteAsInt == 1)
        }
        0x1A -> ResponseInformation(readMqttUtf8StringNotValidated())
        0x1C -> ServerReference(readMqttUtf8StringNotValidated())
        0x1F -> ReasonString(readMqttUtf8StringNotValidated())
        0x21 -> ReceiveMaximum(readUnsignedShort().toInt())
        0x22 -> TopicAlias(readUnsignedShort().toInt())
        0x23 -> TopicAliasMaximum(readUnsignedShort().toInt())
        0x24 -> MaximumQos(if (readByte() == 1.toByte()) QualityOfService.AT_LEAST_ONCE else QualityOfService.AT_MOST_ONCE) // Should not be present for 2
        0x25 -> RetainAvailable(readByte() == 1.toByte())
        0x26 -> UserProperty(
            readMqttUtf8StringNotValidated(),
            readMqttUtf8StringNotValidated()
        )
        0x27 -> MaximumPacketSize(readUnsignedInt().toLong())
        0x28 -> WildcardSubscriptionAvailable(readByte() == 1.toByte())
        0x29 -> SubscriptionIdentifierAvailable(readByte() == 1.toByte())
        0x2A -> SharedSubscriptionAvailable(readByte() == 1.toByte())
        else -> throw MalformedPacketException(
            "Invalid Byte Code while reading properties $identifierByte 0x${identifierByte.toString(
                16
            )}"
        )
    }
    return Pair(property, property.size().toLong() + 1)
}

fun ReadBuffer.readProperties() = readPropertiesSized().second

fun ReadBuffer.readPropertiesSized(): Pair<UInt, Collection<Property>?> {
    val propertyLength = readVariableByteInteger()
    val list = mutableListOf<Property>()
    var totalBytesRead = 0L
    while (totalBytesRead < propertyLength.toInt()) {
        val (property, bytesRead) = readMqttProperty()
        totalBytesRead += bytesRead
        list += property
    }
    return Pair(propertyLength, if (list.isEmpty()) null else list)
}