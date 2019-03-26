@file:Suppress("EXPERIMENTAL_API_USAGE")

package mqtt.wire4.control.packet

import kotlinx.io.core.ByteReadPacket
import kotlinx.io.core.buildPacket
import kotlinx.io.core.readUShort
import kotlinx.io.core.writeUShort
import mqtt.wire.control.packet.format.fixed.DirectionOfFlow

/**
 * 3.6 PUBREL – Publish release (QoS 2 delivery part 2)
 *
 * A PUBREL packet is the response to a PUBREC packet. It is the third packet of the QoS 2 protocol exchange.
 */
data class PublishRelease(val packetIdentifier: UShort)
    : ControlPacket(6, DirectionOfFlow.BIDIRECTIONAL, 0b10) {
    override val variableHeaderPacket: ByteReadPacket = buildPacket { writeUShort(packetIdentifier) }

    companion object {
        fun from(buffer: ByteReadPacket) = PublishRelease(buffer.readUShort())
    }
}
