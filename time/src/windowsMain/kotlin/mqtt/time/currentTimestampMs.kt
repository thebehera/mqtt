package mqtt.time

import kotlin.system.getTimeMillis

actual fun currentTimestampMs() = getTimeMillis()