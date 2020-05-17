@file:Suppress("EXPERIMENTAL_API_USAGE")

package mqtt.wire4.control.packet

import mqtt.buffer.ReadBuffer
import mqtt.buffer.WriteBuffer
import mqtt.wire.control.packet.IPublishRelease
import mqtt.wire.control.packet.format.fixed.DirectionOfFlow

/**
 * 3.6 PUBREL – Publish release (QoS 2 delivery part 2)
 *
 * A PUBREL packet is the response to a PUBREC packet. It is the third packet of the QoS 2 protocol exchange.
 */
data class PublishRelease(override val packetIdentifier: Int)
    : ControlPacketV4(6, DirectionOfFlow.BIDIRECTIONAL, 0b10), IPublishRelease {
    override fun variableHeader(writeBuffer: WriteBuffer) {
        writeBuffer.write(packetIdentifier.toUShort())
    }

    override fun expectedResponse() = PublishComplete(packetIdentifier)

    companion object {
        fun from(buffer: ReadBuffer) = PublishRelease(buffer.readUnsignedShort().toInt())
    }
}
