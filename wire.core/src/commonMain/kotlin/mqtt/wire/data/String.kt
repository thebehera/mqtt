@file:Suppress("EXPERIMENTAL_API_USAGE", "EXPERIMENTAL_FEATURE_WARNING")

package mqtt.wire.data

import kotlinx.io.core.*
import mqtt.wire.MalformedPacketException
import mqtt.wire.data.topic.Filter

fun String.validateMqttUTF8String(): Boolean {
    if (length > 65_535) {
        return false
    }
    for (c in this) {
        if (c in controlCharactersRange) return false
        if (c in shouldNotIncludeCharRange1) return false
        if (c in shouldNotIncludeCharRange2) return false
        if (c in privateUseCharRange) return false
    }
    return true
}

class InvalidMqttUtf8StringMalformedPacketException(msg: String, indexOfError: Int, originalString: String)
    :MalformedPacketException("Fails to match MQTT Spec for a UTF-8 String. Error:($msg) at index $indexOfError of $originalString")

private val controlCharactersRange by lazy { '\uD800'..'\uDFFF' }
private val shouldNotIncludeCharRange1 by lazy { '\u0001'..'\u001F' }
private val shouldNotIncludeCharRange2 by lazy { '\u007F'..'\u009F' }
/**
 * Cannot add planes 15 or 16 as it does not compile into a 'char' in kotlin
 * http://www.unicode.org/faq/private_use.html#pua2
 */
private val privateUseCharRange by lazy { '\uE000'..'\uF8FF' }

inline class MqttUtf8String(private val value: String) {
    fun getValueOrThrow(includeWarnings: Boolean = true): String {
        val ex = exception
        if (ex != null) {
            throw ex
        }
        val w = warning
        if (w != null && includeWarnings) {
            throw w
        }
        return value
    }

    private val exception: InvalidMqttUtf8StringMalformedPacketException?
        get() {
            if (value.length > 65_535) {
                return InvalidMqttUtf8StringMalformedPacketException("MQTT UTF-8 String too large", 65_535, value.substring(0, 65_535))
            }
            value.forEachIndexed { index, c ->
                if (c == '\u0000')
                    return InvalidMqttUtf8StringMalformedPacketException("Invalid Control Character null \\u0000", index, value)
                if (c in controlCharactersRange)
                    return InvalidMqttUtf8StringMalformedPacketException("Invalid Control Character (\\uD800..\\uDFFF)", index, value)
            }
            return null
    }

    private val warning: InvalidMqttUtf8StringMalformedPacketException?
        get() {
            value.forEachIndexed { index, c ->
                if (c in shouldNotIncludeCharRange1)
                    return InvalidMqttUtf8StringMalformedPacketException("Invalid Character in range (\\u0001..\\u001F)", index, value)
                if (c in shouldNotIncludeCharRange2)
                    return InvalidMqttUtf8StringMalformedPacketException("Invalid Character in range (\\u007F..\\u009F)", index, value)
                if (c in privateUseCharRange)
                    return InvalidMqttUtf8StringMalformedPacketException("Invalid Character in range (\\uE000..\\uF8FF)", index, value)
            }
            return null
        }
}

fun BytePacketBuilder.writeMqttUtf8String(string: MqttUtf8String) {
    val validatedString = string.getValueOrThrow()
    val len = validatedString.length.toUShort()
    writeUShort(len)
    writeStringUtf8(validatedString)
}


fun BytePacketBuilder.writeMqttFilter(string: Filter) {
    val validatedString = string.validate()!!.getAllBottomLevelChildren().first().toString()
    val len = validatedString.length.toUShort()
    writeUShort(len)
    writeStringUtf8(validatedString)
}

fun ByteReadPacket.readMqttUtf8String() :MqttUtf8String {
    val ushort = readUShort()
    val stringLength = ushort.toInt()
    if (stringLength == 0) {
        return MqttUtf8String("")
    }
    val text = readTextExactBytes(bytes = stringLength)
    return MqttUtf8String(text)
}

fun ByteReadPacket.readMqttFilter(): Filter {
    val ushort = readUShort()
    val stringLength = ushort.toInt()
    if (stringLength == 0) {
        return Filter("")
    }
    val text = readTextExactBytes(bytes = stringLength)
    return Filter(text)
}

fun ByteReadPacket.readMqttBinary() :ByteArray {
    val stringLength = readUShort().toInt()
    return readBytesOf(max = stringLength)
}
