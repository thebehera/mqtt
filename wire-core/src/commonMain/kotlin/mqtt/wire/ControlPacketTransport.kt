@file:Suppress("EXPERIMENTAL_API_USAGE")

package mqtt.wire

import mqtt.buffer.ReadBuffer
import mqtt.wire.control.packet.ControlPacket
import mqtt.wire.control.packet.ControlPacketFactory

fun ReadBuffer.read(factory: ControlPacketFactory): ControlPacket {
    val byte1 = readUnsignedByte()
    if (!ControlPacket.isValidFirstByte(byte1)) {
        throw MalformedPacketException("Invalid MQTT Control Packet Type: $byte1 Should be in range between 0 and 15 inclusive")
    }
    val remainingLength = readVariableByteInteger()
    return factory.from(this, byte1, remainingLength)
}