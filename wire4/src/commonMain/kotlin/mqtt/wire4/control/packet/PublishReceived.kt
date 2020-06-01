@file:Suppress("EXPERIMENTAL_API_USAGE")

package mqtt.wire4.control.packet

import mqtt.buffer.ReadBuffer
import mqtt.buffer.WriteBuffer
import mqtt.wire.control.packet.IPublishReceived
import mqtt.wire.control.packet.format.fixed.DirectionOfFlow

/**
 * 3.5 PUBREC – Publish received (QoS 2 delivery part 1)
 *
 * A PUBREC packet is the response to a PUBLISH packet with QoS 2. It is the second packet of the QoS 2 protocol exchange.
 */
data class PublishReceived(override val packetIdentifier: Int) : ControlPacketV4(5, DirectionOfFlow.BIDIRECTIONAL),
    IPublishReceived {

    override fun variableHeader(writeBuffer: WriteBuffer) {
        writeBuffer.write(packetIdentifier.toUShort())
    }

    override fun expectedResponse() = PublishRelease(packetIdentifier.toUShort().toInt())

    companion object {
        fun from(buffer: ReadBuffer) = PublishReceived(buffer.readUnsignedShort().toInt())
    }
}
