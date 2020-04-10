@file:Suppress("EXPERIMENTAL_API_USAGE")

package mqtt.wire.control.packet

import kotlinx.io.core.ByteReadPacket
import kotlinx.io.core.buildPacket
import kotlinx.io.core.writeUByte
import mqtt.Parcelable
import mqtt.buffer.WriteBuffer
import mqtt.wire.MqttWarning
import mqtt.wire.control.packet.format.fixed.DirectionOfFlow
import mqtt.wire.data.VariableByteInteger

interface ControlPacket : Parcelable {
    val controlPacketValue: Byte
    val direction: DirectionOfFlow
    val flags: Byte get() = 0b0
    val mqttVersion: Byte

    val controlPacketReader: ControlPacketReader

    private fun fixedHeader(): ByteReadPacket {
        val packetValue = controlPacketValue
        val packetValueUInt = packetValue.toUInt()
        val packetValueShifted = packetValueUInt.shl(4)
        val localFlagsByte = flags.toUByte().toInt()
        val byte1 = (packetValueShifted.toByte() + localFlagsByte).toUByte()
        val byte2 = VariableByteInteger(remainingLength())
        return buildPacket {
            writeUByte(byte1)
            writePacket(byte2.encodedValue())
        }
    }

    private fun fixedHeader(writeBuffer: WriteBuffer) {
        val packetValueUInt = controlPacketValue.toUInt()
        val packetValueShifted = packetValueUInt.shl(4)
        val localFlagsByte = flags.toUByte().toInt()
        val byte1 = (packetValueShifted.toByte() + localFlagsByte).toUByte()
        writeBuffer.write(byte1)
        writeBuffer.writeVariableByteInteger(remainingLength(writeBuffer))
    }

    val variableHeaderPacket: ByteReadPacket? get() = null
    fun variableHeader(writeBuffer: WriteBuffer) {}
    val payloadPacketSize: UInt get() = payloadPacket(false)?.remaining?.toUInt() ?: 0.toUInt()
    fun payloadPacket(sendDefaults: Boolean = false): ByteReadPacket? = null
    fun payload(writeBuffer: WriteBuffer) {}
    fun remainingLength(): UInt {
        val variableHeaderSize = variableHeaderPacket?.copy()?.remaining?.toUInt() ?: 0.toUInt()
        return variableHeaderSize + payloadPacketSize
    }

    fun remainingLength(buffer: WriteBuffer) = remainingLength()

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
        val fixedHeaderPacket = fixedHeader()
        val p = buildPacket {
            writePacket(fixedHeaderPacket)
            if (variableHeaderPacket != null) {
                writePacket(variableHeaderPacket)
            }
            if (payloadPacketSize > 0.toUInt()) {
                writePacket(payloadPacket(sendDefaults)!!)
            }
        }
        return p
    }

    fun serialize(writeBuffer: WriteBuffer) {
        fixedHeader(writeBuffer)
        variableHeader(writeBuffer)
        payload(writeBuffer)
    }

    companion object {
        fun isValidFirstByte(uByte: UByte): Boolean {
            val byte1AsUInt = uByte.toUInt()
            return byte1AsUInt.shr(4).toInt() in 1..15
        }
    }
}
