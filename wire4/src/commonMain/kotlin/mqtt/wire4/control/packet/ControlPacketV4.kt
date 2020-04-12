@file:Suppress("EXPERIMENTAL_API_USAGE")

package mqtt.wire4.control.packet

import mqtt.Ignore
import mqtt.Parcelable
import mqtt.buffer.ReadBuffer
import mqtt.wire.MalformedPacketException
import mqtt.wire.control.packet.ControlPacket
import mqtt.wire.control.packet.format.fixed.DirectionOfFlow

/**
 * The MQTT specification defines fifteen different types of MQTT Control Packet, for example the PublishMessage packet is
 * used to convey Application Messages.
 * @see https://docs.oasis-open.org/mqtt/mqtt/v5.0/cos02/mqtt-v5.0-cos02.html#_Toc1477322
 * @see https://docs.oasis-open.org/mqtt/mqtt/v5.0/mqtt-v5.0.html#_Toc514847903
 * @param controlPacketValue Value defined under [MQTT 2.1.2]
 * @param direction Direction of Flow defined under [MQTT 2.1.2]
 */
abstract class ControlPacketV4(
    @Ignore override val controlPacketValue: Byte,
    @Ignore override val direction: DirectionOfFlow,
    @Ignore override val flags: Byte = 0b0
) : ControlPacket, Parcelable {
    @Ignore
    override val mqttVersion: Byte = 4

    @Ignore
    override val controlPacketReader = ControlPacketV4Reader

    companion object {

        fun from(buffer: ReadBuffer): ControlPacketV4 {
            val byte1 = buffer.readUnsignedByte()
            val remainingLength = buffer.readVariableByteInteger()
            return from(buffer, byte1, remainingLength)
        }

        fun from(buffer: ReadBuffer, byte1: UByte, remainingLength: UInt): ControlPacketV4 {
            val byte1AsUInt = byte1.toUInt()
            val packetValue = byte1AsUInt.shr(4).toInt()
            return when (packetValue) {
                0 -> Reserved
                1 -> ConnectionRequest.from(buffer)
                2 -> ConnectionAcknowledgment.from(buffer)
                3 -> PublishMessage.from(buffer, byte1, remainingLength)
                4 -> PublishAcknowledgment.from(buffer)
                5 -> PublishReceived.from(buffer)
                6 -> PublishRelease.from(buffer)
                7 -> PublishComplete.from(buffer)
                8 -> SubscribeRequest.from(buffer, remainingLength)
                9 -> SubscribeAcknowledgement.from(buffer, remainingLength)
                10 -> UnsubscribeRequest.from(buffer, remainingLength)
                11 -> UnsubscribeAcknowledgment.from(buffer)
                12 -> PingRequest
                13 -> PingResponse
                14 -> DisconnectNotification
                else -> throw MalformedPacketException("Invalid MQTT Control Packet Type: $packetValue Should be in range between 0 and 15 inclusive")
            }
        }
    }
}
