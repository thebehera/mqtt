@file:Suppress("EXPERIMENTAL_API_USAGE")

package mqtt.wire4.control.packet

import kotlinx.io.core.ByteReadPacket
import kotlinx.io.core.buildPacket
import kotlinx.io.core.readUShort
import kotlinx.io.core.writeUShort
import mqtt.wire.control.packet.format.fixed.DirectionOfFlow

/**
 * 3.5 PUBREC – Publish received (QoS 2 delivery part 1)
 *
 * A PUBREC packet is the response to a PUBLISH packet with QoS 2. It is the second packet of the QoS 2 protocol exchange.
 */
data class PublishReceived(val packetIdentifier: UShort)
    : ControlPacket(5, DirectionOfFlow.BIDIRECTIONAL) {
    override val variableHeaderPacket: ByteReadPacket = buildPacket { writeUShort(packetIdentifier) }

    companion object {
        fun from(buffer: ByteReadPacket) = PublishReceived(buffer.readUShort())
    }
}
