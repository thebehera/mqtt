@file:Suppress("EXPERIMENTAL_API_USAGE")

package mqtt.wire.data

import kotlinx.io.core.buildPacket
import kotlinx.io.core.readBytes
import kotlin.experimental.or


fun Int.encodeVariableByteInteger(): ByteArray {
    validateVariableByteInt(this)
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


private val VARIABLE_BYTE_INT_MAX = 268435455
fun validateVariableByteInt(value: Int) {
    if (value in 0..VARIABLE_BYTE_INT_MAX) {
        return
    } else {
        throw IllegalArgumentException("This property must be a number between 0 and $VARIABLE_BYTE_INT_MAX")
    }

}
