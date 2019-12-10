@file:Suppress("EXPERIMENTAL_API_USAGE")

package mqtt.client.transport

import kotlinx.coroutines.io.ByteReadChannel
import kotlinx.coroutines.io.readPacket
import kotlinx.io.core.Input
import mqtt.wire.MalformedInvalidVariableByteInteger
import mqtt.wire.MalformedPacketException
import mqtt.wire.control.packet.ControlPacket
import mqtt.wire.data.VARIABLE_BYTE_INT_MAX
import mqtt.wire4.control.packet.ControlPacketV4
import mqtt.wire5.control.packet.ControlPacketV5
import kotlin.experimental.and

suspend fun ByteReadChannel.read(protocolVersion: Int): ControlPacket {
    val byte1 = readByte().toUByte()
    if (!ControlPacket.isValidFirstByte(byte1)) {
        throw MalformedPacketException("Invalid MQTT Control Packet Type: $byte1 Should be in range between 0 and 15 inclusive")
    }
    val remainingLength = decodeVariableByteInteger().toInt()
    val packet = readPacket(remainingLength)
    return when (protocolVersion) {
        3, 4 -> ControlPacketV4.from(packet, byte1)
        5 -> ControlPacketV5.from(packet, byte1)
        else -> throw IllegalArgumentException("Received an unsupported protocol version $protocolVersion")
    }
}

suspend fun ByteReadChannel.decodeVariableByteInteger(): UInt {
    var digit: Byte
    var value = 0.toUInt()
    var multiplier = 1.toUInt()
    var count = 0.toUInt()
    try {
        do {
            digit = readByte()
            count++
            value += (digit and 0x7F).toUInt() * multiplier
            multiplier *= 128.toUInt()
        } while ((digit and 0x80.toByte()).toInt() != 0)
    } catch (e: Exception) {
        throw MalformedInvalidVariableByteInteger(value)
    }
    if (value < 0.toUInt() || value > VARIABLE_BYTE_INT_MAX.toUInt()) {
        throw MalformedInvalidVariableByteInteger(value)
    }
    return value
}

fun Input.readFirstTwoBytes(): Pair<UByte, Int>? {
    val byte1 = readByte().toUByte()
    if (!ControlPacket.isValidFirstByte(byte1)) {
//        throw MalformedPacketException()
        println("Invalid MQTT Control Packet Type: $byte1 Should be in range between 0 and 15 inclusive")
        return null
    }
    val remainingLength = decodeVariableByteInteger().toInt()
    return Pair(byte1, remainingLength)
}

fun Input.decodeVariableByteInteger(): UInt {
    var digit: Byte
    var value = 0.toUInt()
    var multiplier = 1.toUInt()
    var count = 0.toUInt()
    try {
        do {
            digit = readByte()
            count++
            value += (digit and 0x7F).toUInt() * multiplier
            multiplier *= 128.toUInt()
        } while ((digit and 0x80.toByte()).toInt() != 0)
    } catch (e: Exception) {
        throw MalformedInvalidVariableByteInteger(value)
    }
    if (value < 0.toUInt() || value > VARIABLE_BYTE_INT_MAX.toUInt()) {
        throw MalformedInvalidVariableByteInteger(value)
    }
    return value
}
