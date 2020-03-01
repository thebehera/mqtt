package mqtt.transport.nio2.socket

import mqtt.transport.nio.socket.ByteBufferClientSocket
import mqtt.transport.nio2.util.assignedPort
import java.nio.channels.AsynchronousSocketChannel
import kotlin.time.ExperimentalTime

@ExperimentalUnsignedTypes
@ExperimentalTime
abstract class AsyncBaseClientSocket : ByteBufferClientSocket<AsynchronousSocketChannel>() {
    override fun remotePort() = socket?.assignedPort(remote = true)
}