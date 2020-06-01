@file:Suppress("EXPERIMENTAL_API_USAGE")

package mqtt.wire4.control.packet

import mqtt.buffer.ReadBuffer
import mqtt.buffer.WriteBuffer
import mqtt.wire.control.packet.format.fixed.DirectionOfFlow

data class UnsubscribeAcknowledgment(val packetIdentifier: Int) :
    ControlPacketV4(11, DirectionOfFlow.SERVER_TO_CLIENT) {
    override fun variableHeader(writeBuffer: WriteBuffer) {
        writeBuffer.write(packetIdentifier.toUShort())
    }

    companion object {
        fun from(buffer: ReadBuffer) = UnsubscribeAcknowledgment(buffer.readUnsignedShort().toInt())
    }
}
