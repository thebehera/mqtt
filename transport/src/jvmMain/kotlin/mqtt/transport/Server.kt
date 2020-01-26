package mqtt.transport

import kotlinx.coroutines.CoroutineScope
import mqtt.connection.ServerControlPacketTransport
import mqtt.transport.nio.AsyncServerTransport
import java.nio.channels.AsynchronousChannelGroup
import kotlin.time.ExperimentalTime

@ExperimentalTime
actual fun aServer(scope: CoroutineScope, maxBufferSize: Int, group: Any?): ServerControlPacketTransport =
    AsyncServerTransport(scope, maxBufferSize, group as? AsynchronousChannelGroup)