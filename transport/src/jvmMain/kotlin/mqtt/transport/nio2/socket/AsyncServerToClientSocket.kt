package mqtt.transport.nio2.socket

import kotlinx.coroutines.CoroutineScope
import mqtt.transport.BufferPool
import java.nio.ByteBuffer
import java.nio.channels.AsynchronousSocketChannel
import kotlin.time.Duration
import kotlin.time.ExperimentalTime

@ExperimentalUnsignedTypes
@ExperimentalTime
class AsyncServerToClientSocket(
    scope: CoroutineScope, val asyncSocket: AsynchronousSocketChannel,
    pool: BufferPool<ByteBuffer>,
    readTimeout: Duration,
    writeTimeout: Duration
) : AsyncBaseClientSocket(scope, pool, readTimeout, writeTimeout) {
    init {
        this.socket = asyncSocket
        startWriteChannel()
    }
}