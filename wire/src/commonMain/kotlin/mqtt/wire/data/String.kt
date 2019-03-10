@file:Suppress("EXPERIMENTAL_API_USAGE", "EXPERIMENTAL_FEATURE_WARNING")

package mqtt.wire.data

import kotlinx.io.core.*
import mqtt.wire.MalformedPacketException

fun String.validateMqttUTF8String(): Boolean {
    if (length > 65_535) {
        return false
    }
    val index = indexOfAny(controlCharacters + shouldNotIncludeCharacters + unicodePrivateUseCharacters)
    return index == -1
}

class InvalidMqttUtf8StringMalformedPacketException(msg: String, indexOfError: Int, originalString: String)
    :MalformedPacketException("Fails to match MQTT Spec for a UTF-8 String. Error:($msg) at index $indexOfError of $originalString")

private val controlCharacters by lazy {
    val chars = '\uD800'..'\uDFFF'
    val list = chars.toMutableSet()
    list += '\u0000'
    list.toCharArray()
}

private val shouldNotIncludeCharacters by lazy {
    val u0001To001F = '\u0001'..'\u001F'
    val u0001To001FSet = u0001To001F.toSet()
    val u007FTo009F = '\u007F'..'\u009F'
    val u007FTo009FSet = u007FTo009F.toSet()
    val shouldNotIncludeCharacters = u0001To001FSet + u007FTo009FSet
    shouldNotIncludeCharacters.toCharArray()
}

/**
 * Cannot add planes 15 or 16 as it does not compile into a 'char' in kotlin
 * http://www.unicode.org/faq/private_use.html#pua2
 */
private val unicodePrivateUseCharacters by lazy {
    val mainRange = '\uE000'..'\uF8FF'
    val mainRangeSet = mainRange.toSet()
    mainRangeSet.toCharArray()
}

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
            val index = value.indexOfAny(controlCharacters)
            if (index > -1) {
                return InvalidMqttUtf8StringMalformedPacketException("Invalid Control Character", index, value)
            }
            return null
    }

    private val warning: InvalidMqttUtf8StringMalformedPacketException?
        get() {
            val index = value.indexOfAny(shouldNotIncludeCharacters)
            if (index > -1) {
                return InvalidMqttUtf8StringMalformedPacketException("Disallowed Unicode Code Point", index, value)
            }
            val indexUnicode = value.indexOfAny(unicodePrivateUseCharacters)
            if (indexUnicode > -1) {
                return InvalidMqttUtf8StringMalformedPacketException("Unicode private use character", index, value)
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

fun ByteReadPacket.readMqttUtf8String() :MqttUtf8String {
    val ushort = readUShort()
    val stringLength = ushort.toInt()
    if (stringLength == 0) {
        return MqttUtf8String("")
    }
    val text = readTextExactBytes(bytes = stringLength)
    return MqttUtf8String(text)
}

fun ByteReadPacket.readMqttBinary() :ByteArray {
    val stringLength = readUShort().toInt()
    return readBytesOf(max = stringLength)
}