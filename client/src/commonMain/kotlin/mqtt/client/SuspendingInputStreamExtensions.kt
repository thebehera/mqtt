package mqtt.client

import mqtt.buffer.VARIABLE_BYTE_INT_MAX
import mqtt.socket.SuspendingInputStream
import mqtt.wire.buffer.MalformedInvalidVariableByteInteger
import kotlin.experimental.and
import kotlin.time.ExperimentalTime


@ExperimentalTime
suspend fun SuspendingInputStream.readVariableByteInteger(): UInt {
    var digit: Byte
    var value = 0L
    var multiplier = 1L
    var count = 0
    try {
        do {
            digit = readByte()
            val transformer = transformer
            if (transformer != null) {
                digit = transformer(count.toUInt(), digit)
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
