@file:Suppress("EXPERIMENTAL_API_USAGE")

package mqtt.wire5.control.packet.format.fixed

/**
 * The remaining bits [7-0] of byte can be retreived as a boolean
 * get the value at an index as a boolean
 */
fun UByte.get(index: Int) = this.toInt().and(0b01.shl(index)) != 0
