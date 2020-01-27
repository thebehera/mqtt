@file:Suppress("EXPERIMENTAL_API_USAGE")

package mqtt.wire5.control.packet

import kotlinx.io.core.ByteReadPacket
import kotlinx.io.core.readUByte
import mqtt.wire.MalformedPacketException
import mqtt.wire.control.packet.ControlPacket
import mqtt.wire.data.decodeVariableByteInteger

/**
 * The MQTT specification defines fifteen different types of MQTT Control Packet, for example the PublishMessage packet is
 * used to convey Application Messages.
 * @see https://docs.oasis-open.org/mqtt/mqtt/v5.0/cos02/mqtt-v5.0-cos02.html#_Toc1477322
 * @see https://docs.oasis-open.org/mqtt/mqtt/v5.0/mqtt-v5.0.html#_Toc514847903
 * @param controlPacketValue Value defined under [MQTT 2.1.2]
 * @param direction Direction of Flow defined under [MQTT 2.1.2]
 */
abstract class ControlPacketV5(override val controlPacketValue: Byte,
                               override val direction: mqtt.wire.control.packet.format.fixed.DirectionOfFlow,
                               override val flags: Byte = 0b0) : ControlPacket {
    override val mqttVersion: Byte = 5

    companion object {

        fun from(buffer: ByteReadPacket, throwOnWarning: Boolean = true): ControlPacketV5 {
            val byte1 = buffer.readUByte()
            val remainingStart = buffer.remaining
            val remainingLength = buffer.decodeVariableByteInteger()
            val packet = from(buffer, byte1)
            val afterReadingSize = buffer.remaining
            if (throwOnWarning && afterReadingSize == 0.toLong() && remainingStart - afterReadingSize == remainingLength.toLong()) {
                throw MalformedPacketException("Expected Size does not match the payload")
            }
            return packet
        }

        fun from(buffer: ByteArray, byte1: UByte): ControlPacketV5 {
            return from(ByteReadPacket(buffer), byte1)
        }

        fun from(buffer: ByteReadPacket, byte1: UByte): ControlPacketV5 {
            val byte1AsUInt = byte1.toUInt()
            val packetValue = byte1AsUInt.shr(4).toInt()
            return when (packetValue) {
                0 -> Reserved
                1 -> ConnectionRequest.from(buffer)
                2 -> ConnectionAcknowledgment.from(buffer)
                3 -> PublishMessage.from(buffer, byte1)
                4 -> PublishAcknowledgment.from(buffer)
                5 -> PublishReceived.from(buffer)
                6 -> PublishRelease.from(buffer)
                7 -> PublishComplete.from(buffer)
                8 -> SubscribeRequest.from(buffer)
                9 -> SubscribeAcknowledgement.from(buffer)
                10 -> UnsubscribeRequest.from(buffer)
                11 -> UnsubscribeAcknowledgment.from(buffer)
                12 -> PingRequest
                13 -> PingResponse
                14 -> DisconnectNotification.from(buffer)
                15 -> AuthenticationExchange.from(buffer)
                else -> throw MalformedPacketException("Invalid MQTT Control Packet Type: $packetValue Should be in range between 0 and 15 inclusive")
            }
        }
    }
}
