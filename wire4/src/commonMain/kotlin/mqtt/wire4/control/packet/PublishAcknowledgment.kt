@file:Suppress("EXPERIMENTAL_API_USAGE")

package mqtt.wire4.control.packet

import kotlinx.io.core.ByteReadPacket
import kotlinx.io.core.buildPacket
import kotlinx.io.core.readUShort
import kotlinx.io.core.writeUShort
import mqtt.Parcelize
import mqtt.wire.control.packet.IPublishAcknowledgment
import mqtt.wire.control.packet.format.fixed.DirectionOfFlow

/**
 * 3.4 PUBACK – Publish acknowledgement
 *
 * A PUBACK packet is the response to a PUBLISH packet with QoS 1.
 */
@Parcelize
data class PublishAcknowledgment(override val packetIdentifier: UShort)
    : ControlPacketV4(4, DirectionOfFlow.BIDIRECTIONAL), IPublishAcknowledgment {
    override val variableHeaderPacket: ByteReadPacket = buildPacket { writeUShort(packetIdentifier) }
    companion object {
        fun from(buffer: ByteReadPacket) = PublishAcknowledgment(buffer.readUShort())
    }
}
