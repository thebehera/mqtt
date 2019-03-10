@file:Suppress("EXPERIMENTAL_API_USAGE", "EXPERIMENTAL_FEATURE_WARNING")

package mqtt.wire.data

import kotlinx.io.core.*
import mqtt.wire.MalformedPacketException

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

class InvalidMqttUtf8StringMalformedPacketException(msg: String, indexOfError: Int, originalString: String)
    :MalformedPacketException("Fails to match MQTT Spec for a UTF-8 String. Error:($msg) at index $indexOfError of $originalString")

inline class MqttUtf8String(private val value: String) {
    fun getValueOrThrow() :String {
        val ex = exception
        if (ex != null) {
            throw ex
        }
        return value
    }

    private val exception: InvalidMqttUtf8StringMalformedPacketException?
        get() {
        if (value.length > 65_535) {
            return InvalidMqttUtf8StringMalformedPacketException("MQTT UTF-8 String too large", 65_535, value.substring(0, 65_535))
        }
        var i = 0
        while (i < value.length) {
            val c = value[i]
            val cAsInt = c.toInt()
            // Check for mismatched surrogates
            if (c.isHighSurrogate()) {
                if (++i == value.length) {
                    return InvalidMqttUtf8StringMalformedPacketException("Trailing high surrogate", i, value)
                } else {
                    val c2 = value[i]
                    if (!c2.isLowSurrogate()) {
                        return InvalidMqttUtf8StringMalformedPacketException("No low surrogate", i, value)
                    } else {
                        val ch = cAsInt and 0x3ff shl 10 or (c2.toInt() and 0x3ff)
                        if (ch and 0xffff == 0xffff || ch and 0xffff == 0xfffe) {
                            return InvalidMqttUtf8StringMalformedPacketException("Noncharacter in base plane", i, value)
                        }
                    }
                }
            } else {
                if (c.isISOControl() || c.isLowSurrogate()) {
                    return InvalidMqttUtf8StringMalformedPacketException("Control character or no high surrogate", i, value)
                } else if (cAsInt >= 0xfdd0 && (cAsInt <= 0xfddf || cAsInt >= 0xfffe)) {
                    return InvalidMqttUtf8StringMalformedPacketException("Noncharacter in other nonbase plane", i, value)
                }
            }
            i++
        }
        return null
    }
}

fun BytePacketBuilder.writeMqttUtf8String(string: MqttUtf8String) {
    val validatedString = string.getValueOrThrow()
    writeUShort(validatedString.length.toUShort())
    writeStringUtf8(validatedString)
}

fun ByteReadPacket.readMqttUtf8String() :MqttUtf8String {
    val stringLength = readUShort().toInt()
    if (stringLength == 0) {
        return MqttUtf8String("")
    }
    return MqttUtf8String(readTextExact(stringLength))
}

fun ByteReadPacket.readMqttBinary() :ByteArray {
    val stringLength = readUShort().toInt()
    return readBytesOf(max = stringLength)
}

fun Char.isISOControl() = toInt().isISOControl()
fun Int.isISOControl() = this in 0..0x001F || this in 0x007F..0x009F

