@file:Suppress("EXPERIMENTAL_API_USAGE")

package mqtt.wire.data

import kotlinx.io.core.buildPacket
import kotlinx.io.core.readBytes
import mqtt.wire.MalformedInvalidVariableByteInteger
import kotlin.experimental.and
import kotlin.experimental.or

internal const val VARIABLE_BYTE_INT_MAX = 268435455

fun Int.encodeVariableByteInteger(bypassValidation: Boolean = false): ByteArray {
    if (!bypassValidation) validateVariableByteInt(this)
    var numBytes = 0
    var no = toLong()

    val packet = buildPacket {
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
    return packet.readBytes()
}

fun ByteArray.decodeVariableByteInteger(): Int {
    var digit: Byte
    var value = 0
    var multiplier = 1
    var count = 0
    var byteArrayIndex = 0
    try {
        do {
            digit = get(byteArrayIndex++)
            count++
            value += (digit and 0x7F) * multiplier
            multiplier *= 128
        } while ((digit and 0x80.toByte()).toInt() != 0)
    } catch (e: Exception) {
        throw MalformedInvalidVariableByteInteger(value)
    }
    if (value < 0 || value > VARIABLE_BYTE_INT_MAX) {
        throw MalformedInvalidVariableByteInteger(value)
    }
    return value
}

fun validateVariableByteInt(value: Int) {
    if (value in 0..VARIABLE_BYTE_INT_MAX) {
        return
    } else {
        throw MalformedInvalidVariableByteInteger(value)
    }
}

