@file:Suppress("EXPERIMENTAL_API_USAGE")

package mqtt.client

import kotlinx.coroutines.io.ByteReadChannel
import kotlinx.coroutines.io.ByteWriteChannel
import kotlinx.coroutines.io.readPacket
import mqtt.wire.MalformedInvalidVariableByteInteger
import mqtt.wire.MalformedPacketException
import mqtt.wire.control.packet.ControlPacket
import mqtt.wire.data.VARIABLE_BYTE_INT_MAX
import kotlin.experimental.and

suspend fun ByteReadChannel.read(): ControlPacket {
    val byte1 = readByte().toUByte()
    if (!ControlPacket.isValidFirstByte(byte1)) {
        throw MalformedPacketException("Invalid MQTT Control Packet Type: $byte1 Should be in range between 0 and 15 inclusive")
    }
    val remainingLength = decodeVariableByteInteger().toInt()
    val packet = readPacket(remainingLength)
    return ControlPacket.from(packet, byte1)
}

suspend fun ControlPacket.write(byteWriteChannel: ByteWriteChannel) {
    val data = serialize()
    byteWriteChannel.writePacket(data)
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
