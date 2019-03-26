@file:Suppress("EXPERIMENTAL_API_USAGE")

package mqtt.wire4.control.packet

import kotlinx.io.core.ByteReadPacket
import kotlinx.io.core.buildPacket
import kotlinx.io.core.readUShort
import kotlinx.io.core.writeUShort
import mqtt.wire.ProtocolError
import mqtt.wire.control.packet.format.fixed.DirectionOfFlow
import mqtt.wire.data.MqttUtf8String
import mqtt.wire.data.readMqttUtf8String
import mqtt.wire.data.writeMqttUtf8String

/**
 * 3.10 UNSUBSCRIBE – Unsubscribe request
 * An UNSUBSCRIBE packet is sent by the Client to the Server, to unsubscribe from topics.
 */
data class UnsubscribeRequest(val packetIdentifier: UShort, val topics: List<MqttUtf8String>)
    : ControlPacket(10, DirectionOfFlow.CLIENT_TO_SERVER, 0b10) {
    override val variableHeaderPacket: ByteReadPacket = buildPacket { writeUShort(packetIdentifier) }
    override fun payloadPacket(sendDefaults: Boolean) = buildPacket { topics.forEach { writeMqttUtf8String(it) } }

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
            return UnsubscribeRequest(packetIdentifier, topics)
        }
    }
}
