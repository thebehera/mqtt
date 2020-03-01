package mqtt.transport.nio2.socket

import java.nio.channels.AsynchronousSocketChannel
import kotlin.time.ExperimentalTime

@ExperimentalUnsignedTypes
@ExperimentalTime
class AsyncServerToClientSocket(asyncSocket: AsynchronousSocketChannel) : AsyncBaseClientSocket() {
    init {
        this.socket = asyncSocket
    }
}