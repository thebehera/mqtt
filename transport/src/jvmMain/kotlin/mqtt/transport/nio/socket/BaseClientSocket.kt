package mqtt.transport.nio.socket

import mqtt.transport.BufferPool
import mqtt.transport.nio.socket.util.aClose
import java.net.InetSocketAddress
import java.nio.channels.Selector
import java.nio.channels.SocketChannel
import kotlin.time.ExperimentalTime

@ExperimentalUnsignedTypes
@ExperimentalTime
abstract class BaseClientSocket(pool: BufferPool, protected val blocking: Boolean = false) :
    ByteBufferClientSocket<SocketChannel>(pool) {
    val selector = if (!blocking) Selector.open()!! else null

    override fun remotePort() = (socket?.remoteAddress as? InetSocketAddress)?.port?.toUShort()

    override suspend fun close() {
        selector?.aClose()
        super.close()
    }

}