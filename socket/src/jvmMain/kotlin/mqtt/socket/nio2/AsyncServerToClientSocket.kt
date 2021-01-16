package mqtt.socket.nio2

import java.nio.channels.AsynchronousSocketChannel
import kotlin.time.ExperimentalTime

@ExperimentalUnsignedTypes
@ExperimentalTime
class AsyncServerToClientSocket(asyncSocket: AsynchronousSocketChannel) : AsyncBaseClientSocket() {
    init {
        this.socket = asyncSocket
    }
}