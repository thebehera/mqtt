package mqtt.transport.nio2.util

import kotlinx.io.core.ByteReadPacket
import kotlinx.io.core.use
import mqtt.wire.MalformedInvalidVariableByteInteger
import mqtt.wire.MalformedPacketException
import mqtt.wire.control.packet.ControlPacket
import mqtt.wire.control.packet.IConnectionRequest
import mqtt.wire.data.MqttUtf8String
import mqtt.wire.data.VARIABLE_BYTE_INT_MAX
import mqtt.wire4.control.packet.ControlPacketV4
import mqtt.wire5.control.packet.ControlPacketV5
import java.nio.ByteBuffer
import kotlin.experimental.and

fun ByteBuffer.readConnectionRequest(): IConnectionRequest? {
    flip()
    val position = position()
    val byte1 = get().toUByte()
    if (!ControlPacket.isValidFirstByte(byte1)) {
        throw MalformedPacketException("Invalid MQTT Control Packet Type: ${byte1.toUInt().shr(4).toInt()} Should be in range between 1 and 15 inclusive")
    }
    val remainingLength = decodeVariableByteInteger().toInt()
    if (remainingLength > remaining()) {
        throw NotImplementedError("Not implemented a remaining length larger than ")
    }
    val protocolName = readMqttUtf8String()
    val protocolVersion = get()
    position(position)
    return read(protocolVersion.toUByte().toInt()) as? IConnectionRequest
}

fun ByteBuffer.readMqttUtf8String(): MqttUtf8String {
    val ushort = short.toUShort()
    val stringLength = ushort.toInt()
    if (stringLength == 0) {
        return MqttUtf8String("")
    }
    val position = position()
    val charByteBuffer = Charsets.UTF_8.decode(copy(stringLength))
    position(position + stringLength)
    return MqttUtf8String(charByteBuffer.toString())
}

fun ByteBuffer.copy(size: Int = remaining()): ByteBuffer {
    return ByteBuffer.allocate(size).apply {
        this@copy.slice().moveTo(this@apply)
        clear()
    }
}

fun ByteBuffer.moveTo(destination: ByteBuffer, limit: Int = Int.MAX_VALUE): Int {
    val size = minOf(limit, remaining(), destination.remaining())
    if (size == remaining()) {
        destination.put(this)
    } else {
        val l = limit()
        limit(position() + size)
        destination.put(this)
        limit(l)
    }
    return size
}


fun ByteBuffer.read(protocolVersion: Int): ControlPacket {
    val byte1 = get().toUByte()
    if (!ControlPacket.isValidFirstByte(byte1)) {
        throw MalformedPacketException("Invalid MQTT Control Packet Type: ${byte1.toUInt().shr(4).toInt()} Should be in range between 1 and 15 inclusive")
    }
    val remainingLength = decodeVariableByteInteger().toInt()
    if (remainingLength > remaining()) {
        // we need to increase the buffer at some point in the future
        throw NotImplementedError("Not implemented a remaining length larger than ")
    }
    try {
        ByteReadPacket(this).use { packet ->
            position(position() + remainingLength)
            return when (protocolVersion) {
                3, 4 -> ControlPacketV4.from(packet, byte1)
                5 -> ControlPacketV5.from(packet, byte1)
                else -> throw IllegalArgumentException("Received an unsupported protocol version $protocolVersion")
            }
        }
    } finally {
        compact()
    }
}

fun ByteBuffer.decodeVariableByteInteger(): UInt {
    var digit: Byte
    var value = 0.toUInt()
    var multiplier = 1.toUInt()
    var count = 0.toUInt()
    try {
        do {
            digit = get()
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
