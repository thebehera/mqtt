@file:Suppress("EXPERIMENTAL_API_USAGE", "EXPERIMENTAL_UNSIGNED_LITERALS")

package mqtt.wire5.control.packet.format.variable.property

import kotlinx.io.core.*
import mqtt.buffer.PlatformBuffer
import mqtt.buffer.ReadBuffer
import mqtt.buffer.WriteBuffer
import mqtt.wire.MalformedPacketException
import mqtt.wire.ProtocolError
import mqtt.wire.data.*


abstract class Property(val identifierByte: Byte, val type: Type, val willProperties: Boolean = false) {
    open fun write(bytePacketBuilder: BytePacketBuilder) {}
    open fun write(buffer: WriteBuffer): UInt {
        return 0u
    }

    open fun size(buffer: WriteBuffer): UInt {
        return 0u
    }

    internal fun write(bytePacketBuilder: BytePacketBuilder, boolean: Boolean) {
        bytePacketBuilder.writeByte(identifierByte)
        bytePacketBuilder.writeByte(if (boolean) 1 else 0)
    }

    internal fun write(buffer: WriteBuffer, boolean: Boolean): UInt {
        buffer.write(identifierByte)
        buffer.write((if (boolean) 1 else 0).toUByte())
        return 2u
    }

    fun write(bytePacketBuilder: BytePacketBuilder, number: UInt) {
        bytePacketBuilder.writeByte(identifierByte)
        bytePacketBuilder.writeUInt(number)
    }

    fun size(bytePacketBuilder: WriteBuffer, number: UInt) = 5u
    fun write(bytePacketBuilder: WriteBuffer, number: UInt): UInt {
        bytePacketBuilder.write(identifierByte)
        bytePacketBuilder.write(number)
        return 5u
    }

    fun write(bytePacketBuilder: BytePacketBuilder, number: UShort) {
        bytePacketBuilder.writeByte(identifierByte)
        bytePacketBuilder.writeUShort(number)
    }

    fun size(bytePacketBuilder: WriteBuffer, number: UShort) = 3u
    fun write(bytePacketBuilder: WriteBuffer, number: UShort): UInt {
        bytePacketBuilder.write(identifierByte)
        bytePacketBuilder.write(number)
        return 3u
    }

    fun write(bytePacketBuilder: BytePacketBuilder, p: ByteReadPacket) {
        bytePacketBuilder.writeByte(identifierByte)
        bytePacketBuilder.writePacket(p)
    }

    fun write(bytePacketBuilder: BytePacketBuilder, string: MqttUtf8String) {
        bytePacketBuilder.writeByte(identifierByte)
        bytePacketBuilder.writeMqttUtf8String(string)
    }

    fun size(bytePacketBuilder: WriteBuffer, string: CharSequence) =
        bytePacketBuilder.mqttUtf8Size(string) + 1u

    fun write(bytePacketBuilder: WriteBuffer, string: CharSequence): UInt {
        bytePacketBuilder.write(identifierByte)
        val size = bytePacketBuilder.mqttUtf8Size(string)
        bytePacketBuilder.writeUtf8String(string)
        return size
    }


    fun write(bytePacketBuilder: BytePacketBuilder, data: ByteArrayWrapper) {
        bytePacketBuilder.writeByte(identifierByte)
        bytePacketBuilder.writeUShort(data.byteArray.size.toUShort())
        bytePacketBuilder.writeFully(data.byteArray)
    }

    fun write(bytePacketBuilder: WriteBuffer, data: ByteArrayWrapper): UInt {
        bytePacketBuilder.write(identifierByte)
        bytePacketBuilder.write(data.byteArray.size.toUShort())
        bytePacketBuilder.write(data.byteArray)
        return 3u + data.byteArray.size.toUInt()
    }

    fun size(bytePacketBuilder: WriteBuffer, data: ByteArrayWrapper) = 3u + data.byteArray.size.toUInt()
}

private fun ByteReadPacket.readMqttBinary(): ByteArrayWrapper {
    val dataSize = readUShort()
    return ByteArrayWrapper(readBytes(dataSize.toInt()))
}

fun ByteReadPacket.readMqttProperty(): Pair<Property, Long> {
    val propertyIndexStart = remaining
    val byte = readByte().toInt()
    val property = when (byte) {
        0x01 -> PayloadFormatIndicator(readByte() == 1.toByte())
        0x02 -> MessageExpiryInterval(readUInt().toLong())
        0x03 -> ContentType(readMqttUtf8String())
        0x08 -> ResponseTopic(readMqttUtf8String())
        0x09 -> CorrelationData(readMqttBinary())
        0x0B -> SubscriptionIdentifier(decodeVariableByteInteger().toLong())
        0x11 -> SessionExpiryInterval(readUInt().toLong())
        0x12 -> AssignedClientIdentifier(readMqttUtf8String())
        0x13 -> ServerKeepAlive(readUShort().toInt())
        0x15 -> AuthenticationMethod(readMqttUtf8String())
        0x16 -> AuthenticationData(readMqttBinary())
        0x17 -> {
            val uByteAsInt = readUByte().toInt()
            if (!(uByteAsInt == 0 || uByteAsInt == 1)) {
                throw ProtocolError(
                    "Request Problem Information cannot have a value other than 0 or 1" +
                            "see: https://docs.oasis-open.org/mqtt/mqtt/v5.0/cos02/mqtt-v5.0-cos02.html#_Toc1477353"
                )
            }
            RequestProblemInformation(uByteAsInt == 1)
        }
        0x18 -> WillDelayInterval(readUInt().toLong())
        0x19 -> {
            val uByteAsInt = readUByte().toInt()
            if (!(uByteAsInt == 0 || uByteAsInt == 1)) {
                throw ProtocolError(
                    "Request Response Information cannot have a value other than 0 or 1" +
                            "see: https://docs.oasis-open.org/mqtt/mqtt/v5.0/cos02/mqtt-v5.0-cos02.html#_Toc1477352"
                )
            }
            RequestResponseInformation(uByteAsInt == 1)
        }
        0x1A -> ResponseInformation(readMqttUtf8String())
        0x1C -> ServerReference(readMqttUtf8String())
        0x1F -> ReasonString(readMqttUtf8String())
        0x21 -> ReceiveMaximum(readUShort().toInt())
        0x22 -> TopicAlias(readUShort().toInt())
        0x23 -> TopicAliasMaximum(readUShort().toInt())
        0x24 -> MaximumQos(if (readByte() == 1.toByte()) QualityOfService.AT_LEAST_ONCE else QualityOfService.AT_MOST_ONCE) // Should not be present for 2
        0x25 -> RetainAvailable(readByte() == 1.toByte())
        0x26 -> UserProperty(readMqttUtf8String(), readMqttUtf8String())
        0x27 -> MaximumPacketSize(readUInt().toLong())
        0x28 -> WildcardSubscriptionAvailable(readByte() == 1.toByte())
        0x29 -> SubscriptionIdentifierAvailable(readByte() == 1.toByte())
        0x2A -> SharedSubscriptionAvailable(readByte() == 1.toByte())
        else -> throw MalformedPacketException("Invalid Byte Code while reading properties at index $propertyIndexStart")
    }
    val bytesRead = propertyIndexStart - remaining
    return Pair(property, bytesRead)
}


fun ReadBuffer.readMqttProperty(): Pair<Property, Long> {
    val property = when (readByte().toInt()) {
        0x01 -> {
            PayloadFormatIndicator(readByte() == 1.toByte())
        }
        0x02 -> {
            MessageExpiryInterval(readUnsignedInt().toLong())
        }
        0x03 -> {
            readMqttUtf8StringNotValidated()
            ContentType(MqttUtf8String(readMqttUtf8StringNotValidated()))
        }
        0x08 -> ResponseTopic(MqttUtf8String(readMqttUtf8StringNotValidated()))
        0x09 -> CorrelationData(ByteArrayWrapper(readByteArray(readUnsignedShort().toUInt())))
        0x0B -> SubscriptionIdentifier(readVariableByteInteger().toLong())
        0x11 -> SessionExpiryInterval(readUnsignedInt().toLong())
        0x12 -> AssignedClientIdentifier(MqttUtf8String(readMqttUtf8StringNotValidated()))
        0x13 -> ServerKeepAlive(readUnsignedShort().toInt())
        0x15 -> AuthenticationMethod(MqttUtf8String(readMqttUtf8StringNotValidated()))
        0x16 -> AuthenticationData(ByteArrayWrapper(readByteArray(readUnsignedShort().toUInt())))
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
        0x1A -> ResponseInformation(MqttUtf8String(readMqttUtf8StringNotValidated()))
        0x1C -> ServerReference(MqttUtf8String(readMqttUtf8StringNotValidated()))
        0x1F -> ReasonString(MqttUtf8String(readMqttUtf8StringNotValidated()))
        0x21 -> ReceiveMaximum(readUnsignedShort().toInt())
        0x22 -> TopicAlias(readUnsignedShort().toInt())
        0x23 -> TopicAliasMaximum(readUnsignedShort().toInt())
        0x24 -> MaximumQos(if (readByte() == 1.toByte()) QualityOfService.AT_LEAST_ONCE else QualityOfService.AT_MOST_ONCE) // Should not be present for 2
        0x25 -> RetainAvailable(readByte() == 1.toByte())
        0x26 -> UserProperty(
            MqttUtf8String(readMqttUtf8StringNotValidated()),
            MqttUtf8String(readMqttUtf8StringNotValidated())
        )
        0x27 -> MaximumPacketSize(readUnsignedInt().toLong())
        0x28 -> WildcardSubscriptionAvailable(readByte() == 1.toByte())
        0x29 -> SubscriptionIdentifierAvailable(readByte() == 1.toByte())
        0x2A -> SharedSubscriptionAvailable(readByte() == 1.toByte())
        else -> throw MalformedPacketException("Invalid Byte Code while reading properties")
    }
    return Pair(property, property.size(this as PlatformBuffer).toLong() + 1)
}

fun ByteReadPacket.readPropertiesLegacy(): Collection<Property>? {
    val propertyLength = decodeVariableByteInteger().toInt()
    val list = mutableListOf<Property>()
    var totalBytesRead = 0L
    while (totalBytesRead < propertyLength) {
        val (property, bytesRead) = readMqttProperty()
        totalBytesRead += bytesRead
        list += property
    }
    return if (list.isEmpty()) null else list
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