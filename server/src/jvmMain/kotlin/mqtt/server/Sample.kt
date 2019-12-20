package mqtt.server

import java.math.BigInteger

actual data class SampleNativeClass actual constructor(actual val x: Int) {
    actual fun twoPlusTwo(): Int = (BigInteger("2") + BigInteger("2")).toInt()
}