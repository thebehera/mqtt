package mqtt.socket.nio2

import mqtt.buffer.BufferPool
import java.nio.channels.AsynchronousSocketChannel
import kotlin.time.ExperimentalTime

@ExperimentalUnsignedTypes
@ExperimentalTime
class AsyncServerToClientSocket(asyncSocket: AsynchronousSocketChannel) : AsyncBaseClientSocket(BufferPool()) {
    init {
        this.socket = asyncSocket
    }
}