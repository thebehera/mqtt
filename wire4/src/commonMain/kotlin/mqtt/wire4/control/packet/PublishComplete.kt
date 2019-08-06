@file:Suppress("EXPERIMENTAL_API_USAGE")

package mqtt.wire4.control.packet

import kotlinx.io.core.ByteReadPacket
import kotlinx.io.core.buildPacket
import kotlinx.io.core.readUShort
import kotlinx.io.core.writeUShort
import mqtt.wire.control.packet.IPublishComplete
import mqtt.wire.control.packet.format.fixed.DirectionOfFlow

/**
 * 3.7 PUBCOMP – Publish complete (QoS 2 delivery part 3)
 *
 * The PUBCOMP packet is the response to a PUBREL packet. It is the fourth and final packet of the QoS 2 protocol exchange.
 */
data class PublishComplete(override val packetIdentifier: UShort)
    : ControlPacketV4(7, DirectionOfFlow.BIDIRECTIONAL), IPublishComplete {
    override val variableHeaderPacket: ByteReadPacket = buildPacket { writeUShort(packetIdentifier) }

    companion object {
        fun from(buffer: ByteReadPacket) = PublishComplete(buffer.readUShort())
    }
}
