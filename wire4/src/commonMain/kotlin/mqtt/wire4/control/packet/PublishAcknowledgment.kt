@file:Suppress("EXPERIMENTAL_API_USAGE")

package mqtt.wire4.control.packet

import kotlinx.io.core.ByteReadPacket
import kotlinx.io.core.buildPacket
import kotlinx.io.core.readUShort
import kotlinx.io.core.writeUShort
import mqtt.wire.control.packet.format.fixed.DirectionOfFlow

/**
 * 3.4 PUBACK – Publish acknowledgement
 *
 * A PUBACK packet is the response to a PUBLISH packet with QoS 1.
 */
data class PublishAcknowledgment(val packetIdentifier: UShort)
    : ControlPacket(4, DirectionOfFlow.BIDIRECTIONAL) {
    override val variableHeaderPacket: ByteReadPacket = buildPacket { writeUShort(packetIdentifier) }

    companion object {
        fun from(buffer: ByteReadPacket) = PublishAcknowledgment(buffer.readUShort())
    }
}
