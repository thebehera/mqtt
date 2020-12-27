@file:Suppress("EXPERIMENTAL_API_USAGE", "EXPERIMENTAL_UNSIGNED_LITERALS")

package mqtt.wire4.control.packet

import mqtt.buffer.ReadBuffer
import mqtt.buffer.WriteBuffer
import mqtt.wire.ProtocolError
import mqtt.wire.control.packet.IUnsubscribeRequest
import mqtt.wire.control.packet.format.fixed.DirectionOfFlow
import mqtt.wire.data.MqttUtf8String
import mqtt.wire.data.utf8Length

/**
 * 3.10 UNSUBSCRIBE – Unsubscribe request
 * An UNSUBSCRIBE packet is sent by the Client to the Server, to unsubscribe from topics.
 */
data class UnsubscribeRequest(
    override val packetIdentifier: Int,
    val topics: List<MqttUtf8String>
) : ControlPacketV4(10, DirectionOfFlow.CLIENT_TO_SERVER, 0b10), IUnsubscribeRequest {
    override fun remainingLength() = UShort.SIZE_BYTES.toUInt() + payloadSize()


    override fun variableHeader(writeBuffer: WriteBuffer) {
        writeBuffer.write(packetIdentifier.toUShort())
    }

    private fun payloadSize(): UInt {
        var size = 0u
        topics.forEach {
            size += UShort.SIZE_BYTES.toUInt() + it.value.utf8Length().toUInt()
        }
        return size
    }

    override fun payload(writeBuffer: WriteBuffer) {
        topics.forEach { writeBuffer.writeMqttUtf8String(it.value) }
    }

    init {
        if (topics.isEmpty()) {
            throw ProtocolError("An UNSUBSCRIBE packet with no Payload is a Protocol Error")
        }
    }

    companion object {
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
