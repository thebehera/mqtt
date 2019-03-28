@file:Suppress("EXPERIMENTAL_API_USAGE")

package mqtt.wire.control.packet

import kotlinx.io.core.ByteReadPacket
import kotlinx.io.core.buildPacket
import kotlinx.io.core.writeUByte
import mqtt.wire.MqttWarning
import mqtt.wire.control.packet.format.fixed.DirectionOfFlow
import mqtt.wire.data.VariableByteInteger

interface ControlPacket {
    val controlPacketValue: Byte
    val direction: DirectionOfFlow
    val flags: Byte get() = 0b0
    val mqttVersion: Byte

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

    val variableHeaderPacket: ByteReadPacket? get() = null
    fun payloadPacket(sendDefaults: Boolean = false): ByteReadPacket? = null
    private fun remainingLength(payloadSize: Int = 0): UInt {
        val variableHeaderSize = variableHeaderPacket?.remaining ?: 0
        return (variableHeaderSize + payloadSize).toUInt()
    }

    fun validateOrGetWarning(): MqttWarning? {
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
        fun isValidFirstByte(uByte: UByte): Boolean {
            val byte1AsUInt = uByte.toUInt()
            return byte1AsUInt.shr(4).toInt() in 1..15
        }
    }
}
