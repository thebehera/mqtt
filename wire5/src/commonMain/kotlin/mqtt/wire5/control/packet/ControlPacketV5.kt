@file:Suppress("EXPERIMENTAL_API_USAGE")

package mqtt.wire5.control.packet

import mqtt.buffer.ReadBuffer
import mqtt.wire.MalformedPacketException
import mqtt.wire.control.packet.ControlPacket

/**
 * The MQTT specification defines fifteen different types of MQTT Control Packet, for example the PublishMessage packet is
 * used to convey Application Messages.
 * @see https://docs.oasis-open.org/mqtt/mqtt/v5.0/cos02/mqtt-v5.0-cos02.html#_Toc1477322
 * @see https://docs.oasis-open.org/mqtt/mqtt/v5.0/mqtt-v5.0.html#_Toc514847903
 * @param controlPacketValue Value defined under [MQTT 2.1.2]
 * @param direction Direction of Flow defined under [MQTT 2.1.2]
 */
abstract class ControlPacketV5(
    override val controlPacketValue: Byte,
    override val direction: mqtt.wire.control.packet.format.fixed.DirectionOfFlow,
    override val flags: Byte = 0b0
) : ControlPacket {
    override val mqttVersion: Byte = 5

    override val controlPacketReader = ControlPacketV5Reader

    companion object {

        fun from(buffer: ReadBuffer): ControlPacketV5 {
            val byte1 = buffer.readUnsignedByte()
            val remainingLength = buffer.readVariableByteInteger()
            return from(buffer, byte1, remainingLength)
        }

        fun from(buffer: ReadBuffer, byte1: UByte, remainingLength: UInt): ControlPacketV5 {
            val byte1AsUInt = byte1.toUInt()
            val packetValue = byte1AsUInt.shr(4).toInt()
            return when (packetValue) {
                0 -> Reserved
                1 -> ConnectionRequest.from(buffer)
                2 -> ConnectionAcknowledgment.from(buffer, remainingLength)
                3 -> PublishMessage.from(buffer, byte1, remainingLength)
                4 -> PublishAcknowledgment.from(buffer, remainingLength)
                5 -> PublishReceived.from(buffer, remainingLength)
                6 -> PublishRelease.from(buffer, remainingLength)
                7 -> PublishComplete.from(buffer, remainingLength)
                8 -> SubscribeRequest.from(buffer, remainingLength)
                9 -> SubscribeAcknowledgement.from(buffer, remainingLength)
                10 -> UnsubscribeRequest.from(buffer, remainingLength)
                11 -> UnsubscribeAcknowledgment.from(buffer, remainingLength)
                12 -> PingRequest
                13 -> PingResponse
                14 -> DisconnectNotification.from(buffer)
                15 -> AuthenticationExchange.from(buffer)
                else -> throw MalformedPacketException("Invalid MQTT Control Packet Type: $packetValue Should be in range between 0 and 15 inclusive")
            }
        }
    }
}
