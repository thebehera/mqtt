@file:Suppress("EXPERIMENTAL_API_USAGE")

package mqtt.wire4.control.packet

import kotlinx.io.core.*
import mqtt.Parcelize
import mqtt.wire.MalformedPacketException
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
@Parcelize
data class SubscribeAcknowledgement(override val packetIdentifier: UShort, val payload: List<ReasonCode>)
    : ControlPacketV4(9, DirectionOfFlow.SERVER_TO_CLIENT), ISubscribeAcknowledgement {
    override val variableHeaderPacket: ByteReadPacket = buildPacket { writeUShort(packetIdentifier) }
    override fun payloadPacket(sendDefaults: Boolean) = buildPacket { payload.forEach { writeUByte(it.byte) } }

    companion object {
        fun from(buffer: ByteReadPacket): SubscribeAcknowledgement {
            val packetIdentifier = buffer.readUShort()
            val returnCodes = mutableListOf<ReasonCode>()
            while (buffer.remaining > 0) {
                val reasonCode = when (val reasonCodeByte = buffer.readUByte()) {
                    GRANTED_QOS_0.byte -> GRANTED_QOS_0
                    GRANTED_QOS_1.byte -> GRANTED_QOS_1
                    GRANTED_QOS_2.byte -> GRANTED_QOS_2
                    UNSPECIFIED_ERROR.byte -> UNSPECIFIED_ERROR
                    else -> throw MalformedPacketException("Invalid return code $reasonCodeByte")
                }
                returnCodes += reasonCode
            }
            return SubscribeAcknowledgement(packetIdentifier, returnCodes)
        }
    }
}
