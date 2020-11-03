package mqtt.buffer

import kotlin.experimental.and

@ExperimentalUnsignedTypes
interface ReadBuffer {

    fun limit(): UInt
    fun position(): UInt
    fun position(newPosition: Int)
    fun remaining() = limit() - position()
    fun hasRemaining() = position() < limit()

    fun resetForRead()
    fun readByte(): Byte
    fun readByteArray(size: UInt): ByteArray
    fun readUnsignedByte(): UByte
    fun readUnsignedShort(): UShort
    fun readUnsignedInt(): UInt
    fun readLong(): Long
    fun readUtf8(bytes: UInt): CharSequence
    fun readUtf8(bytes: Int): CharSequence = readUtf8(bytes.toUInt())
    fun readUtf8Line(): CharSequence {
        val initialPosition = position()
        var lastByte: Byte = 0
        var currentByte: Byte = 0
        var bytesRead = 0u
        while (remaining() > 0u) {
            lastByte = currentByte
            currentByte = readByte()
            bytesRead++
            if (currentByte == newLine[1]) {
                break
            }
        }
        val carriageFeedPositionIncrement =
            if (lastByte == newLine[0] && currentByte == newLine[1]) 2
            else if (currentByte == newLine[1]) 1
            else 0

        val bytesToRead = bytesRead - carriageFeedPositionIncrement.toUInt()
        position(initialPosition.toInt())
        val result = readUtf8(bytesToRead)
        position(position().toInt() + carriageFeedPositionIncrement)
        return result
    }

    fun readMqttUtf8StringNotValidated(): CharSequence = readMqttUtf8StringNotValidatedSized().second

    fun readMqttUtf8StringNotValidatedSized(): Pair<UInt, CharSequence> {
        val length = readUnsignedShort().toUInt()
        val decoded = readUtf8(length)
        return Pair(length, decoded)
    }

    fun readGenericType(deserializationParameters: DeserializationParameters) =
        GenericSerialization.deserialize(deserializationParameters)

    fun readVariableByteInteger(): UInt {
        var digit: Byte
        var value = 0L
        var multiplier = 1L
        var count = 0L
        try {
            do {
                digit = readByte()
                count++
                value += (digit and 0x7F).toLong() * multiplier
                multiplier *= 128
            } while ((digit and 0x80.toByte()).toInt() != 0)
        } catch (e: Exception) {
            throw MalformedInvalidVariableByteInteger(value.toUInt())
        }
        if (value < 0 || value > VARIABLE_BYTE_INT_MAX.toLong()) {
            throw MalformedInvalidVariableByteInteger(value.toUInt())
        }
        return value.toUInt()
    }

    fun variableByteSize(uInt: UInt): UByte {
        if (uInt !in 0.toUInt()..VARIABLE_BYTE_INT_MAX) {
            throw MalformedInvalidVariableByteInteger(uInt)
        }
        var numBytes = 0
        var no = uInt.toLong()
        do {
            no /= 128
            numBytes++
        } while (no > 0 && numBytes < 4)
        return numBytes.toUByte()
    }

    companion object {
        val newLine = "\r\n".encodeToByteArray()
    }
}