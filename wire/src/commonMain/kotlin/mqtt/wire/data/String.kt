@file:Suppress("EXPERIMENTAL_API_USAGE")

package mqtt.wire.data

import kotlinx.io.core.*

fun String.validateMqttUTF8String(): Boolean {
    var i = 0
    while (i < length) {
        var isBad = false
        val c = get(i)
        val cAsInt = c.toInt()
        /* Check for mismatched surrogates */
        if (c.isHighSurrogate()) {
            if (++i == length) {
                isBad = true /* Trailing high surrogate */
            } else {
                val c2 = get(i)
                if (!c2.isLowSurrogate()) {
                    isBad = true /* No low surrogate */
                } else {
                    val ch = cAsInt and 0x3ff shl 10 or (c2.toInt() and 0x3ff)
                    if (ch and 0xffff == 0xffff || ch and 0xffff == 0xfffe) {
                        isBad = true /* Noncharacter in base plane */
                    }
                }
            }
        } else {
            if (c.isISOControl() || c.isLowSurrogate()) {
                isBad = true /* Control character or no high surrogate */
            } else if (cAsInt >= 0xfdd0 && (cAsInt <= 0xfddf || cAsInt >= 0xfffe)) {
                isBad = true /* Noncharacter in other nonbase plane */
            }
        }
        if (isBad) {
            return false
        }
        i++
    }
    return true
}

fun String.toMqttUtf8Encoded(): ByteArray {
    if (!validateMqttUTF8String()) {
        throw IllegalArgumentException("Invalid utf-8 string")
    }
    val bytes = toByteArray()
    if (bytes.size > 65_535) {
        throw IllegalArgumentException("UTF-8 String too large to meet spec")
    }
    val size = bytes.size.toUShort()
    val packet = buildPacket {
        writeUShort(size)
        writeFully(bytes)
    }
    return packet.readBytes()
}

fun Char.isISOControl() = toInt().isISOControl()
fun Int.isISOControl() = this in 0..0x001F || this in 0x007F..0x009F

