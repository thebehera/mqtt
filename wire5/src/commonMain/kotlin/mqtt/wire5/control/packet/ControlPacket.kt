@file:Suppress("EXPERIMENTAL_API_USAGE")

package mqtt.wire5.control.packet

import kotlinx.io.core.ByteReadPacket
import kotlinx.io.core.buildPacket
import kotlinx.io.core.readUByte
import kotlinx.io.core.writeUByte
import mqtt.wire.MalformedPacketException
import mqtt.wire.MqttWarning
import mqtt.wire.data.VariableByteInteger
import mqtt.wire.data.decodeVariableByteInteger
import mqtt.wire5.control.packet.format.fixed.DirectionOfFlow

/**
 * The MQTT specification defines fifteen different types of MQTT Control Packet, for example the PublishMessage packet is
 * used to convey Application Messages.
 * @see https://docs.oasis-open.org/mqtt/mqtt/v5.0/cos02/mqtt-v5.0-cos02.html#_Toc1477322
 * @see https://docs.oasis-open.org/mqtt/mqtt/v5.0/mqtt-v5.0.html#_Toc514847903
 * @param controlPacketValue Value defined under [MQTT 2.1.2]
 * @param direction Direction of Flow defined under [MQTT 2.1.2]
 */
abstract class ControlPacket(val controlPacketValue: Byte,
                             val direction: DirectionOfFlow,
                             val flags: Byte = 0b0) {

    private fun fixedHeader(payloadSize: Int = 0): ByteReadPacket {
        val packetValue = controlPacketValue
        val packetValueUInt = packetValue.toUInt()
        val packetValueShifted = packetValueUInt.shl(4)
        val localFlagsByte = flags.toUByte().toInt()
        val byte1 = (packetValueShifted.toByte() + localFlagsByte).toUByte()
        val byte2 = VariableByteInteger(payloadSize.toUInt())
        return buildPacket {
            writeUByte(byte1)
            writePacket(byte2.encodedValue())
        }
    }

    open val variableHeaderPacket: ByteReadPacket? = null
    open fun payloadPacket(sendDefaults: Boolean = false): ByteReadPacket? = null
    private fun remainingLength(payloadSize: Int = 0): UInt {
        val variableHeaderSize = variableHeaderPacket?.remaining ?: 0
        return (variableHeaderSize + payloadSize).toUInt()
    }

    open fun validateOrGetWarning(): MqttWarning? {
        return null
    }

    /**
     * Create a byte array representing the control packet
     * @param sendDefaults Increase the data transferred by defining the default explicitly
     */
    fun serialize(sendDefaults: Boolean = false, throwOnWarning: Boolean = true): ByteReadPacket {
        val warning = validateOrGetWarning()
        if (warning != null && throwOnWarning) {
            throw warning
        }
        val variableHeaderPacket = variableHeaderPacket
        val variablePacketSize = variableHeaderPacket?.remaining ?: 0L
        val payloadPacket = payloadPacket(sendDefaults)
        val payloadPacketSize = payloadPacket?.remaining ?: 0L
        val fixedHeaderPacket = fixedHeader((variablePacketSize + payloadPacketSize).toInt())
        val p = buildPacket {
            writePacket(fixedHeaderPacket)
            if (variableHeaderPacket != null) {
                writePacket(variableHeaderPacket)
            }
            if (payloadPacket != null) {
                writePacket(payloadPacket)
            }
        }
        return p
    }
    companion object {
//        fun from(buffer: ByteReadPacket): ControlPacket {
//            val byte1 = buffer.readUByte()
//            val byte1AsUInt = byte1.toUInt()
//            val packetValue = byte1AsUInt.shr(4).toInt()
//            val remainingLength = buffer.decodeVariableByteInteger() // remaining Length
//            val packet = when (packetValue) {
//                0 -> Reserved
//                1 -> ConnectionRequest.from(buffer)
//                2 -> ConnectionAcknowledgment.from(buffer)
//                3 -> PublishMessage.from(buffer, byte1)
//                4 -> PublishAcknowledgment.from(buffer)
//                5 -> PublishReceived.from(buffer)
//                6 -> PublishRelease.from(buffer)
//                7 -> PublishComplete.from(buffer)
//                8 -> SubscribeRequest.from(buffer)
//                9 -> SubscribeAcknowledgement.from(buffer)
//                10 -> UnsubscribeRequest.from(buffer)
//                11 -> UnsubscribeAcknowledgment.from(buffer)
//                12 -> PingRequest
//                13 -> PingResponse
//                14 -> DisconnectNotification.from(buffer)
//                15 -> AuthenticationExchange.from(buffer)
//                else -> throw MalformedPacketException("Invalid MQTT Control Packet Type: $packetValue Should be in range between 0 and 15 inclusive")
//            }
//            val afterReadingSize = buffer.remaining
//            if (afterReadingSize != remainingLength.toLong()) {
//                packet.toString()
//            }
//            return packet
//        }

        fun from(buffer: ByteReadPacket, throwOnWarning: Boolean = true): ControlPacket {
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
        fun from(buffer:ByteReadPacket, byte1: UByte): ControlPacket {
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
        fun isValidFirstByte(uByte: UByte) :Boolean {
            val byte1AsUInt = uByte.toUInt()
            return byte1AsUInt.shr(4).toInt() in 1..15
        }
    }
}
