@file:Suppress("EXPERIMENTAL_API_USAGE")

package mqtt.wire.control.packet.format.fixed

import mqtt.wire.MalformedPacketException
import mqtt.wire.data.QualityOfService

/**
 * The remaining bits [3-0] of byte 1 in the Fixed Header contain flags specific to each MQTT Control Packet type
 * @see https://docs.oasis-open.org/mqtt/mqtt/v5.0/cos02/mqtt-v5.0-cos02.html#_Toc1477323
 */
data class FlagBits(val bit3: Boolean = false,
                    val bit2: Boolean = false,
                    val bit1: Boolean = false,
                    val bit0: Boolean = false) {
    constructor(dup: Boolean, qos: QualityOfService, retain: Boolean)
            : this(dup, qos.toBitInformation().first, qos.toBitInformation().second, retain)
}
internal val emptyFlagBits by lazy { FlagBits() }
internal val bit1TrueFlagBits by lazy { FlagBits(bit1 = true) }

fun FlagBits.toByte() = booleanArrayOf(bit0, bit1, bit2, bit3).toByte()
fun UByte.toFlagBits() :FlagBits {
    return when (this) {
        0x00.toUByte() -> emptyFlagBits
        0x01.toUByte() -> FlagBits(bit0 = true)
        0x02.toUByte() -> bit1TrueFlagBits
        0x03.toUByte() -> FlagBits(bit1 = true, bit0 = true)
        0x04.toUByte() -> FlagBits(bit2 = true)
        0x05.toUByte() -> FlagBits(bit2 = true, bit0 = true)
//        0x06.toUByte() -> FlagBits(bit2 = true, bit1 = true) // QOS cannot be greater than 0x02
//        0x07.toUByte() -> FlagBits(bit2 = true, bit1 = true, bit0 = true) // QOS cannot be greater than 0x02
        0x08.toUByte() -> FlagBits(bit3 = true)
        0x09.toUByte() -> FlagBits(bit3 = true, bit0 = true)
        0x10.toUByte() -> FlagBits(bit3 = true, bit1 = true)
        0x11.toUByte() -> FlagBits(bit3 = true, bit1 = true, bit0 = true)
        0x12.toUByte() -> FlagBits(bit3 = true, bit2 = true)
        0x13.toUByte() -> FlagBits(bit3 = true, bit2 = true, bit0 = true)
//        0x14.toUByte() -> FlagBits(bit3 = true, bit2 = true, bit1 = true) // QOS cannot be greater than 0x02
//        0x15.toUByte() -> FlagBits(bit3 = true, bit2 = true, bit1 = true, bit0 = true) // QOS cannot be greater than 0x02
        else -> throw MalformedPacketException("Invalid flags received, 0x${this.toInt().toString(16)}. Double check QOS is not set to 0x03")
    }
}

/**
 * The remaining bits [7-0] of byte can be retreived as a boolean
 * get the value at an index as a boolean
 */
fun UByte.get(index: Int) = toInt().shl(index - 1) == 1

/**
 * Bits in a byte are labelled 7 to 0. Bit number 7 is the most significant bit, the least significant bit is assigned
 * bit number 0.
 */
fun BooleanArray.toByte(): Byte {
    if (size > 8) {
        throw IllegalArgumentException("Cannot pack information into a byte. Boolean Array too large")
    }
    var result = 0
    forEachIndexed { index, it ->
        if (it) {
            result = result or (1 shl index)
        }
    }
    return result.toByte()
}
