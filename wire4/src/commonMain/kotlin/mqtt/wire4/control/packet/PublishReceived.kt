@file:Suppress("EXPERIMENTAL_API_USAGE")

package mqtt.wire4.control.packet

import kotlinx.io.core.ByteReadPacket
import kotlinx.io.core.buildPacket
import kotlinx.io.core.readUShort
import kotlinx.io.core.writeUShort
import mqtt.Parcelize
import mqtt.wire.control.packet.IPublishReceived
import mqtt.wire.control.packet.format.fixed.DirectionOfFlow

/**
 * 3.5 PUBREC – Publish received (QoS 2 delivery part 1)
 *
 * A PUBREC packet is the response to a PUBLISH packet with QoS 2. It is the second packet of the QoS 2 protocol exchange.
 */
@Parcelize
data class PublishReceived(override val packetIdentifier: Int)
    : ControlPacketV4(5, DirectionOfFlow.BIDIRECTIONAL), IPublishReceived {
    override val variableHeaderPacket: ByteReadPacket = buildPacket { writeUShort(packetIdentifier.toUShort()) }

    override fun expectedResponse() = PublishRelease(packetIdentifier.toUShort().toInt())
    companion object {
        fun from(buffer: ByteReadPacket) = PublishReceived(buffer.readUShort().toInt())
    }
}
