@file:Suppress("EXPERIMENTAL_API_USAGE", "EXPERIMENTAL_UNSIGNED_LITERALS")

package mqtt.wire4.control.packet

import mqtt.buffer.ReadBuffer
import mqtt.buffer.WriteBuffer
import mqtt.wire.MalformedPacketException
import mqtt.wire.buffer.variableByteSize
import mqtt.wire.control.packet.ISubscribeAcknowledgement
import mqtt.wire.control.packet.format.ReasonCode
import mqtt.wire.control.packet.format.ReasonCode.*
import mqtt.wire.control.packet.format.fixed.DirectionOfFlow

/**
 * 3.9 SUBACK – Subscribe acknowledgement
 *
 * A SUBACK Packet is sent by the Server to the Client to confirm receipt and processing of a SUBSCRIBE Packet.
 *
 * A SUBACK Packet contains a list of return codes, that specify the maximum QoS level that was granted in each
 * Subscription that was requested by the SUBSCRIBE.
 */
data class SubscribeAcknowledgement(override val packetIdentifier: Int, val payload: List<ReasonCode>) :
    ControlPacketV4(9, DirectionOfFlow.SERVER_TO_CLIENT), ISubscribeAcknowledgement {
    override fun remainingLength() = 2u + payload.size.toUInt()
    override fun variableHeader(writeBuffer: WriteBuffer) {
        writeBuffer.write(packetIdentifier.toUShort())
    }

    override fun payload(writeBuffer: WriteBuffer) {
        payload.forEach { writeBuffer.write(it.byte.toUByte()) }
    }

    companion object {

        fun from(buffer: ReadBuffer, remainingLength: UInt): SubscribeAcknowledgement {
            val packetIdentifier = buffer.readUnsignedShort()
            val returnCodes = mutableListOf<ReasonCode>()
            while (returnCodes.size.toUInt() < remainingLength - variableByteSize(remainingLength) - 1u) {
                val reasonCode = when (val reasonCodeByte = buffer.readUnsignedByte()) {
                    GRANTED_QOS_0.byte -> GRANTED_QOS_0
                    GRANTED_QOS_1.byte -> GRANTED_QOS_1
                    GRANTED_QOS_2.byte -> GRANTED_QOS_2
                    UNSPECIFIED_ERROR.byte -> UNSPECIFIED_ERROR
                    else -> throw MalformedPacketException("Invalid return code $reasonCodeByte")
                }
                returnCodes += reasonCode
            }
            return SubscribeAcknowledgement(packetIdentifier.toInt(), returnCodes)
        }
    }
}
