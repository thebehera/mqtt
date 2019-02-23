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

fun FlagBits.toByte(): Byte {
    val booleanArray = booleanArrayOf(bit0, bit1, bit2, bit3)
    var result = 0
    booleanArray.forEachIndexed { index, it ->
        if (it) {
            result = result or (1 shl index)
        }
    }
    return result.toByte()
}
