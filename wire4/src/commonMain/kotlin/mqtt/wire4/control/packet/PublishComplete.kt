@file:Suppress("EXPERIMENTAL_API_USAGE")

package mqtt.wire4.control.packet

import mqtt.buffer.ReadBuffer
import mqtt.buffer.WriteBuffer
import mqtt.wire.control.packet.IPublishComplete
import mqtt.wire.control.packet.format.fixed.DirectionOfFlow

/**
 * 3.7 PUBCOMP – Publish complete (QoS 2 delivery part 3)
 *
 * The PUBCOMP packet is the response to a PUBREL packet. It is the fourth and final packet of the QoS 2 protocol exchange.
 */
data class PublishComplete(override val packetIdentifier: Int)
    : ControlPacketV4(7, DirectionOfFlow.BIDIRECTIONAL), IPublishComplete {
    override fun variableHeader(writeBuffer: WriteBuffer) {
        writeBuffer.write(packetIdentifier.toUShort())
    }

    companion object {
        fun from(buffer: ReadBuffer) = PublishComplete(buffer.readUnsignedShort().toInt())
    }
}
