@file:Suppress("EXPERIMENTAL_API_USAGE")

package mqtt.wire.control.packet.format.fixed

/**
 * The remaining bits [7-0] of byte can be retreived as a boolean
 * get the value at an index as a boolean
 */
fun UByte.get(index: Int) = this.toInt().and(0b01.shl(index)) != 0

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
