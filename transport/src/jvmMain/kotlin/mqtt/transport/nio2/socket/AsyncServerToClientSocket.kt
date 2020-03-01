package mqtt.transport.nio2.socket

import mqtt.transport.BufferPool
import java.nio.channels.AsynchronousSocketChannel
import kotlin.time.ExperimentalTime

@ExperimentalUnsignedTypes
@ExperimentalTime
class AsyncServerToClientSocket(
    asyncSocket: AsynchronousSocketChannel,
    pool: BufferPool
) : AsyncBaseClientSocket(pool) {
    init {
        this.socket = asyncSocket
    }
}