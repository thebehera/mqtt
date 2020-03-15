@file:Suppress("EXPERIMENTAL_API_USAGE")

package mqtt.wire4.control.packet

import kotlinx.io.core.ByteReadPacket
import kotlinx.io.core.buildPacket
import kotlinx.io.core.readUShort
import kotlinx.io.core.writeUShort
import mqtt.IgnoredOnParcel
import mqtt.Parcelize
import mqtt.buffer.ReadBuffer
import mqtt.buffer.WriteBuffer
import mqtt.wire.ProtocolError
import mqtt.wire.control.packet.IUnsubscribeRequest
import mqtt.wire.control.packet.format.fixed.DirectionOfFlow

import mqtt.wire.data.MqttUtf8String
import mqtt.wire.data.readMqttUtf8String
import mqtt.wire.data.writeMqttUtf8String

/**
 * 3.10 UNSUBSCRIBE – Unsubscribe request
 * An UNSUBSCRIBE packet is sent by the Client to the Server, to unsubscribe from topics.
 */
@Parcelize
data class UnsubscribeRequest(
    val packetIdentifier: Int,
    val topics: List<MqttUtf8String>)
    : ControlPacketV4(10, DirectionOfFlow.CLIENT_TO_SERVER, 0b10), IUnsubscribeRequest {
    @IgnoredOnParcel
    override val variableHeaderPacket: ByteReadPacket = buildPacket { writeUShort(packetIdentifier.toUShort()) }
    override fun payloadPacket(sendDefaults: Boolean) = buildPacket { topics.forEach { writeMqttUtf8String(it) } }
    override fun variableHeader(writeBuffer: WriteBuffer) {
        writeBuffer.write(packetIdentifier.toUShort())
    }

    override fun payload(writeBuffer: WriteBuffer) {
        topics.forEach { writeBuffer.writeUtf8String(it.value) }
    }

    init {
        if (topics.isEmpty()) {
            throw ProtocolError("An UNSUBSCRIBE packet with no Payload is a Protocol Error")
        }
    }

    companion object {
        fun from(buffer: ByteReadPacket): UnsubscribeRequest {
            val packetIdentifier = buffer.readUShort()
            val topics = mutableListOf<MqttUtf8String>()
            while (buffer.remaining > 0) {
                topics += buffer.readMqttUtf8String()
            }
            return UnsubscribeRequest(packetIdentifier.toInt(), topics)
        }

        fun from(buffer: ReadBuffer, remainingLength: UInt): UnsubscribeRequest {
            val packetIdentifier = buffer.readUnsignedShort()
            val topics = mutableListOf<MqttUtf8String>()
            var bytesRead = 0
            while (bytesRead.toUInt() < remainingLength - 2u) {
                val pair = buffer.readMqttUtf8StringNotValidatedSized()
                bytesRead += 2 + pair.first.toInt()
                topics += MqttUtf8String(pair.second)
            }
            return UnsubscribeRequest(packetIdentifier.toInt(), topics)
        }
    }
}
