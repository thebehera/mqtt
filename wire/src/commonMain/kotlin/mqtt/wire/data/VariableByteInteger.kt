@file:Suppress("EXPERIMENTAL_API_USAGE", "EXPERIMENTAL_FEATURE_WARNING")

package mqtt.wire.data

import kotlinx.io.core.ByteReadPacket
import kotlinx.io.core.buildPacket
import mqtt.wire.MalformedInvalidVariableByteInteger
import kotlin.experimental.and
import kotlin.experimental.or

internal val VARIABLE_BYTE_INT_MAX = 268435455.toUInt()

fun ByteReadPacket.decodeVariableByteInteger() :UInt {
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

fun ByteArray.decodeVariableByteInteger(): UInt {
    var digit: Byte
    var value = 0.toUInt()
    var multiplier = 1.toUInt()
    var count = 0.toUInt()
    var byteArrayIndex = 0
    try {
        do {
            digit = get(byteArrayIndex++)
            count++
            value += (digit and 0x7F).toUInt() * multiplier
            multiplier *= 128.toUInt()
        } while ((digit and 0x80.toByte()).toInt() != 0)
    } catch (e: Exception) {
        throw MalformedInvalidVariableByteInteger(value.toUInt())
    }
    if (value < 0.toUInt() || value > VARIABLE_BYTE_INT_MAX) {
        throw MalformedInvalidVariableByteInteger(value.toUInt())
    }
    return value
}

inline class VariableByteInteger(val value: UInt) {

    fun encodedValue() :ByteReadPacket {
        if (value !in 0.toUInt()..VARIABLE_BYTE_INT_MAX.toUInt()) {
            throw MalformedInvalidVariableByteInteger(value)
        }
        var numBytes = 0
        var no = value.toLong()
        return buildPacket {
            do {
                var digit = (no % 128).toByte()
                no /= 128
                if (no > 0) {
                    digit = digit or 0x80.toByte()
                }
                writeByte(digit)
                numBytes++
            } while (no > 0 && numBytes < 4)
        }
    }
}

