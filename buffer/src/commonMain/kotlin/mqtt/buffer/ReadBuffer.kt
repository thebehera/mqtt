package mqtt.buffer

import kotlin.experimental.and
import kotlin.experimental.or

@ExperimentalUnsignedTypes
interface ReadBuffer {
    fun resetForRead()
    fun readByte(): Byte
    fun readByteArray(size: UInt): ByteArray
    fun readUnsignedByte(): UByte
    fun readUnsignedShort(): UShort
    fun readUnsignedInt(): UInt
    fun readMqttUtf8StringNotValidated(): CharSequence = readMqttUtf8StringNotValidatedSized().second
    fun readMqttUtf8StringNotValidatedSized(): Pair<UInt, CharSequence>

    fun readVariableByteInteger(): UInt {
        var digit: Byte
        var value = 0.toUInt()
        var multiplier = 1.toUInt()
        var count = 0.toUInt()
        try {
            do {
                digit = readByte()
                count++
                value += (digit and 0x7F).toUInt() * multiplier
                multiplier *= 128.toUInt()
            } while ((digit and 0x80.toByte()).toInt() != 0)
        } catch (e: Exception) {
            throw MalformedInvalidVariableByteInteger(value)
        }
        if (value < 0.toUInt() || value > VARIABLE_BYTE_INT_MAX.toUInt()) {
            throw MalformedInvalidVariableByteInteger(value)
        }
        return value
    }

    fun variableByteSize(uInt: UInt): UByte {
        if (uInt !in 0.toUInt()..VARIABLE_BYTE_INT_MAX.toUInt()) {
            throw MalformedInvalidVariableByteInteger(uInt)
        }
        var numBytes = 0
        var no = uInt.toLong()
        do {
            var digit = (no % 128).toByte()
            no /= 128
            if (no > 0) {
                digit = digit or 0x80.toByte()
            }
            numBytes++
        } while (no > 0 && numBytes < 4)
        return numBytes.toUByte()
    }

//    fun <T> readTyped(deserializationStrategy: MqttDeserializationStrategy<T>): T
    // mqtt 5
    // fun readProperty():Property
}