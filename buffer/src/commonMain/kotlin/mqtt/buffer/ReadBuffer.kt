package mqtt.buffer

import kotlin.experimental.and

@ExperimentalUnsignedTypes
interface ReadBuffer {
    fun resetForRead()
    fun readByte(): Byte
    fun readByteArray(size: UInt): ByteArray
    fun readUnsignedByte(): UByte
    fun readUnsignedShort(): UShort
    fun readUnsignedInt(): UInt
    fun readLong(): Long
    fun readUtf8(bytes: UInt): CharSequence
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
        if (uInt !in 0.toUInt()..VARIABLE_BYTE_INT_MAX.toUInt()) {
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

    fun sizeUtf8String(
        inputSequence: CharSequence,
        malformedInput: CharSequence? = null,
        unmappableCharacter: CharSequence? = null
    ): UInt

}