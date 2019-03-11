@file:Suppress("EXPERIMENTAL_API_USAGE")

package mqtt.wire.control.packet.format.variable.property

import kotlinx.io.core.*
import mqtt.wire.MalformedPacketException
import mqtt.wire.ProtocolError
import mqtt.wire.data.*

abstract class Property(val identifierByte: Byte, val type: Type, val willProperties: Boolean = false) {
    open fun write(bytePacketBuilder: BytePacketBuilder) {}
    internal fun write(bytePacketBuilder: BytePacketBuilder, boolean: Boolean) {
        bytePacketBuilder.writeByte(identifierByte)
        bytePacketBuilder.writeByte(if (boolean) 1 else 0)
    }

    fun write(bytePacketBuilder: BytePacketBuilder, number: UInt) {
        bytePacketBuilder.writeByte(identifierByte)
        bytePacketBuilder.writeUInt(number)
    }

    fun write(bytePacketBuilder: BytePacketBuilder, number: UShort) {
        bytePacketBuilder.writeByte(identifierByte)
        bytePacketBuilder.writeUShort(number)
    }

    fun write(bytePacketBuilder: BytePacketBuilder, p: ByteReadPacket) {
        bytePacketBuilder.writeByte(identifierByte)
        bytePacketBuilder.writePacket(p)
    }

    fun write(bytePacketBuilder: BytePacketBuilder, string: MqttUtf8String) {
        bytePacketBuilder.writeByte(identifierByte)
        bytePacketBuilder.writeMqttUtf8String(string)
    }

    fun write(bytePacketBuilder: BytePacketBuilder, data: ByteArrayWrapper) {
        bytePacketBuilder.writeByte(identifierByte)
        bytePacketBuilder.writeUShort(data.byteArray.size.toUShort())
        bytePacketBuilder.writeFully(data.byteArray)
    }
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
        0x02 -> MessageExpiryInterval(readUInt())
        0x03 -> ContentType(readMqttUtf8String())
        0x08 -> ResponseTopic(readMqttUtf8String())
        0x09 -> CorrelationData(readMqttBinary())
        0x0B -> SubscriptionIdentifier(decodeVariableByteInteger())
        0x11 -> SessionExpiryInterval(readUInt())
        0x12 -> AssignedClientIdentifier(readMqttUtf8String())
        0x13 -> ServerKeepAlive(readUShort())
        0x15 -> AuthenticationMethod(readMqttUtf8String())
        0x16 -> AuthenticationData(readMqttBinary())
        0x17 -> {
            val uByteAsInt = readUByte().toInt()
            if (!(uByteAsInt == 0 || uByteAsInt == 1)) {
                throw ProtocolError("Request Problem Information cannot have a value other than 0 or 1" +
                        "see: https://docs.oasis-open.org/mqtt/mqtt/v5.0/cos02/mqtt-v5.0-cos02.html#_Toc1477353")
            }
            RequestProblemInformation(uByteAsInt == 1)
        }
        0x18 -> WillDelayInterval(readUInt())
        0x19 -> {
            val uByteAsInt = readUByte().toInt()
            if (!(uByteAsInt == 0 || uByteAsInt == 1)) {
                throw ProtocolError("Request Response Information cannot have a value other than 0 or 1" +
                        "see: https://docs.oasis-open.org/mqtt/mqtt/v5.0/cos02/mqtt-v5.0-cos02.html#_Toc1477352")
            }
            RequestResponseInformation(uByteAsInt == 1)
        }
        0x1A -> ResponseInformation(readMqttUtf8String())
        0x1C -> ServerReference(readMqttUtf8String())
        0x1F -> ReasonString(readMqttUtf8String())
        0x21 -> ReceiveMaximum(readUShort())
        0x22 -> TopicAliasMaximum(readUShort())
        0x23 -> TopicAlias(readUShort())
        0x24 -> MaximumQos(if (readByte() == 1.toByte()) QualityOfService.AT_LEAST_ONCE else QualityOfService.AT_MOST_ONCE) // Should not be present for 2
        0x25 -> RetainAvailable(readByte() == 1.toByte())
        0x26 -> UserProperty(readMqttUtf8String(), readMqttUtf8String())
        0x27 -> MaximumPacketSize(readUInt())
        0x28 -> WildcardSubscriptionAvailable(readByte() == 1.toByte())
        0x29 -> SubscriptionIdentifierAvailable(readByte() == 1.toByte())
        0x2A -> SharedSubscriptionAvailable(readByte() == 1.toByte())
        else -> throw MalformedPacketException("Invalid Byte Code while reading properties at index $propertyIndexStart")
    }
    val bytesRead = propertyIndexStart - remaining
    return Pair(property, bytesRead)
}

fun ByteReadPacket.readProperties(): Collection<Property>? {
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
