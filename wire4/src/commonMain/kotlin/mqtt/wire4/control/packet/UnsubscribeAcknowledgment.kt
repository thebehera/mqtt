@file:Suppress("EXPERIMENTAL_API_USAGE")

package mqtt.wire4.control.packet

import kotlinx.io.core.ByteReadPacket
import kotlinx.io.core.buildPacket
import kotlinx.io.core.readUShort
import kotlinx.io.core.writeUShort
import mqtt.IgnoredOnParcel
import mqtt.Parcelize
import mqtt.buffer.ReadBuffer
import mqtt.buffer.WriteBuffer
import mqtt.wire.control.packet.format.fixed.DirectionOfFlow

@Parcelize
data class UnsubscribeAcknowledgment(val packetIdentifier: Int) :
    ControlPacketV4(11, DirectionOfFlow.SERVER_TO_CLIENT) {
    @IgnoredOnParcel
    override val variableHeaderPacket: ByteReadPacket = buildPacket { writeUShort(packetIdentifier.toUShort()) }
    override fun variableHeader(writeBuffer: WriteBuffer) {
        writeBuffer.write(packetIdentifier.toUShort())
    }

    companion object {
        fun from(buffer: ByteReadPacket) = UnsubscribeAcknowledgment(buffer.readUShort().toInt())
        fun from(buffer: ReadBuffer) = UnsubscribeAcknowledgment(buffer.readUnsignedShort().toInt())
    }
}
