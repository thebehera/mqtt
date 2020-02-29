package mqtt.transport.nio.socket

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import mqtt.time.currentTimestampMs
import mqtt.transport.BufferPool
import mqtt.transport.ClientToServerSocket
import mqtt.transport.nio.socket.util.aConfigureBlocking
import mqtt.transport.nio.socket.util.connect
import mqtt.transport.nio.socket.util.openSocketChannel
import mqtt.transport.util.asInetAddress
import java.net.InetSocketAddress
import kotlin.time.Duration
import kotlin.time.ExperimentalTime

@ExperimentalUnsignedTypes
@ExperimentalCoroutinesApi
@ExperimentalTime
class NioClientSocket(
    coroutineScope: CoroutineScope,
    pool: BufferPool,
    val blocking: Boolean = true,
    readTimeout: Duration,
    writeTimeout: Duration
) : BaseClientSocket(coroutineScope, pool, readTimeout, writeTimeout), ClientToServerSocket {

    override suspend fun open(hostname: String?, port: UShort) {
        val socketAddress = InetSocketAddress(hostname?.asInetAddress(), port.toInt())
        val socketChannel = openSocketChannel()
        socketChannel.aConfigureBlocking(blocking)
        this.socket = socketChannel
        if (!socketChannel.connect(scope, socketAddress, selector, readTimeout)) {
            println("\"${currentTimestampMs()} $tag  FAILED TO CONNECT CLIENT client ${(socketChannel.remoteAddress as? InetSocketAddress)?.port} $socketChannel")
        }
        startWriteChannel()
    }
}


