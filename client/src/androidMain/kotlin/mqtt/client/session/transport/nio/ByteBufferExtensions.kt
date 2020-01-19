package mqtt.client.session.transport.nio

import kotlinx.io.core.ByteReadPacket
import kotlinx.io.core.use
import mqtt.wire.MalformedInvalidVariableByteInteger
import mqtt.wire.MalformedPacketException
import mqtt.wire.control.packet.ControlPacket
import mqtt.wire.control.packet.IDisconnectNotification
import mqtt.wire.control.packet.IPingRequest
import mqtt.wire.data.VARIABLE_BYTE_INT_MAX
import mqtt.wire4.control.packet.ControlPacketV4
import mqtt.wire4.control.packet.DisconnectNotification
import mqtt.wire4.control.packet.PingRequest
import mqtt.wire5.control.packet.ControlPacketV5
import java.nio.ByteBuffer
import kotlin.experimental.and


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

fun disconnect(protocolVersion: Int): IDisconnectNotification {
    return when (protocolVersion) {
        3, 4 -> DisconnectNotification
        5 -> mqtt.wire5.control.packet.DisconnectNotification()
        else -> throw IllegalArgumentException("Received an unsupported protocol version $protocolVersion")
    }
}

fun ping(protocolVersion: Int): IPingRequest {
    return when (protocolVersion) {
        3, 4 -> PingRequest
        5 -> mqtt.wire5.control.packet.PingRequest
        else -> throw IllegalArgumentException("Received an unsupported protocol version $protocolVersion")
    }
}

fun ByteBuffer.decodeVariableByteInteger(): UInt {
    var digit: Byte
    var value = 0.toUInt()
    var multiplier = 1.toUInt()
    var count = 0.toUInt()
    try {
        do {
            println("getting digit ${remaining()}")
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
