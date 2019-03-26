@file:Suppress("EXPERIMENTAL_API_USAGE")

package mqtt.wire4.control.packet

import kotlinx.io.core.ByteReadPacket
import kotlinx.io.core.buildPacket
import kotlinx.io.core.readUShort
import kotlinx.io.core.writeUShort
import mqtt.wire.control.packet.format.fixed.DirectionOfFlow

data class UnsubscribeAcknowledgment(val packetIdentifier: UShort)
    : ControlPacket(11, DirectionOfFlow.SERVER_TO_CLIENT) {
    override val variableHeaderPacket: ByteReadPacket = buildPacket { writeUShort(packetIdentifier) }

    companion object {
        fun from(buffer: ByteReadPacket) = UnsubscribeAcknowledgment(buffer.readUShort())
    }
}
