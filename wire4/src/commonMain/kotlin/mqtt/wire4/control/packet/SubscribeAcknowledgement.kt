@file:Suppress("EXPERIMENTAL_API_USAGE")

package mqtt.wire4.control.packet

import kotlinx.io.core.*
import mqtt.wire.MalformedPacketException
import mqtt.wire.control.packet.ISubscribeAcknowledgement
import mqtt.wire.control.packet.format.fixed.DirectionOfFlow
import mqtt.wire4.control.packet.SubscribeAcknowledgement.ReturnCode.*

/**
 * 3.9 SUBACK – Subscribe acknowledgement
 *
 * A SUBACK Packet is sent by the Server to the Client to confirm receipt and processing of a SUBSCRIBE Packet.
 *
 * A SUBACK Packet contains a list of return codes, that specify the maximum QoS level that was granted in each
 * Subscription that was requested by the SUBSCRIBE.
 */
data class SubscribeAcknowledgement(override val packetIdentifier: UShort, val payload: List<ReturnCode>)
    : ControlPacketV4(9, DirectionOfFlow.SERVER_TO_CLIENT), ISubscribeAcknowledgement {
    override val variableHeaderPacket: ByteReadPacket = buildPacket { writeUShort(packetIdentifier) }
    override fun payloadPacket(sendDefaults: Boolean) = buildPacket { payload.forEach { writeUByte(it.byte) } }

    companion object {
        fun from(buffer: ByteReadPacket): SubscribeAcknowledgement {
            val packetIdentifier = buffer.readUShort()
            val returnCodes = mutableListOf<ReturnCode>()
            while (buffer.remaining > 0) {
                val reasonCodeByte = buffer.readUByte()
                val returnCode = when (reasonCodeByte) {
                    SUCCESS_MAXIMUM_QOS_0.byte -> SUCCESS_MAXIMUM_QOS_0
                    SUCCESS_MAXIMUM_QOS_1.byte -> SUCCESS_MAXIMUM_QOS_1
                    SUCCESS_MAXIMUM_QOS_2.byte -> SUCCESS_MAXIMUM_QOS_2
                    FAILURE.byte -> FAILURE
                    else -> throw MalformedPacketException("Invalid return code $reasonCodeByte " +
                            "see: https://docs.oasis-open.org/mqtt/mqtt/v5.0/cos02/mqtt-v5.0-cos02.html#_Toc1477478")
                }
                returnCodes += returnCode
            }
            return SubscribeAcknowledgement(packetIdentifier, returnCodes)
        }
    }

    enum class ReturnCode(val byte: UByte) {
        SUCCESS_MAXIMUM_QOS_0(0x0.toUByte()),
        SUCCESS_MAXIMUM_QOS_1(0x01.toUByte()),
        SUCCESS_MAXIMUM_QOS_2(0x02.toUByte()),
        FAILURE(0x80.toUByte())
    }
}


private val validSubscribeCodes by lazy {
    setOf(SUCCESS_MAXIMUM_QOS_0)
}
