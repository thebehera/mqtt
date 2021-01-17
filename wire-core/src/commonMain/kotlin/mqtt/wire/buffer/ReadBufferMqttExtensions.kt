package mqtt.wire.buffer

import mqtt.buffer.PlatformBuffer
import mqtt.buffer.ReadBuffer
import mqtt.buffer.VARIABLE_BYTE_INT_MAX
import kotlin.experimental.and


fun ReadBuffer.readMqttUtf8StringNotValidated(): CharSequence = readMqttUtf8StringNotValidatedSized().second

fun ReadBuffer.readMqttUtf8StringNotValidatedSized(): Pair<UInt, CharSequence> {
    val length = readUnsignedShort().toUInt()
    val decoded = readUtf8(length)
    return Pair(length, decoded)
}

fun readGenericType(deserializationParameters: DeserializationParameters) =
    GenericSerialization.deserialize(deserializationParameters)

sealed class VariableByteIntegerRead {
    class NotEnoughSpaceInBuffer(val bytesRead: ByteArray) : VariableByteIntegerRead() {
        class Result(val remainingLength: UInt, val bytesReadFromBuffer: Int)

        fun getRemainingLengthWithNextBuffer(nextBuffer: PlatformBuffer): Result {
            val remainingLength = nextBuffer.readVariableByteStartingFromArray(bytesRead)
            return Result(remainingLength, 4 - bytesRead.count())
        }
    }

    class SuccessfullyRead(val variableByteInteger: UInt) : VariableByteIntegerRead()
}

fun ReadBuffer.tryReadingVariableByteInteger(): VariableByteIntegerRead {
    var digit: Byte
    var value = 0L
    var multiplier = 1L
    var count = 0L
    val digits = ByteArray(4)
    try {
        do {
            if (!hasRemaining()) {
                return VariableByteIntegerRead.NotEnoughSpaceInBuffer(ByteArray(count.toInt()) { digits[it] })
            }
            digit = readByte()
            digits[count.toInt()] = digit
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
    return VariableByteIntegerRead.SuccessfullyRead(value.toUInt())
}

fun ReadBuffer.readVariableByteStartingFromArray(array: ByteArray): UInt {
    var digit: Byte
    var value = 0L
    var multiplier = 1L
    var count = 0L
    try {
        do {
            digit = if (count.toInt() < array.count()) {
                array[count.toInt()]
            } else {
                readByte()
            }
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

fun ReadBuffer.readVariableByteInteger(): UInt {
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