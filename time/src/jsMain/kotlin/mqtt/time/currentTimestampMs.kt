package mqtt.time

import kotlin.js.Date

actual fun currentTimestampMs() = Date.now().toLong()