package mqtt.wire.data

fun Int.isTwoByteInt() = this in 0..65535
