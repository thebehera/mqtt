package mqtt.transport

import kotlinx.coroutines.CoroutineScope
import mqtt.connection.ServerControlPacketTransport
import kotlin.time.ExperimentalTime

@ExperimentalTime
expect fun aServer(
    scope: CoroutineScope,
    maxBufferSize: Int,
    group: Any?
): ServerControlPacketTransport