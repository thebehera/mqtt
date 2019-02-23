package mqtt.wire.control.packet.fixed

/**
 * The remaining bits [3-0] of byte 1 in the Fixed Header contain flags specific to each MQTT Control Packet type
 * @see https://docs.oasis-open.org/mqtt/mqtt/v5.0/cos02/mqtt-v5.0-cos02.html#_Toc1477323
 */
data class FlagBits(val bit3: Boolean = false,
                    val bit2: Boolean = false,
                    val bit1: Boolean = false,
                    val bit0: Boolean = false)

internal val emptyFlagBits by lazy { FlagBits() }
internal val bit1TrueFlagBits by lazy { FlagBits(bit1 = true) }

fun FlagBits.toByte() = booleanArrayOf(bit0, bit1, bit2, bit3).toByte()

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
