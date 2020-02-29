package mqtt.transport.nio2.socket

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import mqtt.transport.BufferPool
import mqtt.transport.ClientToServerSocket
import mqtt.transport.nio.socket.util.asyncSetOption
import mqtt.transport.nio2.util.aConnect
import mqtt.transport.nio2.util.asyncSocket
import mqtt.transport.util.asInetAddress
import java.net.InetSocketAddress
import java.net.StandardSocketOptions
import kotlin.time.Duration
import kotlin.time.ExperimentalTime

@ExperimentalUnsignedTypes
@ExperimentalCoroutinesApi
@ExperimentalTime
class AsyncClientSocket(
    coroutineScope: CoroutineScope,
    pool: BufferPool,
    readTimeout: Duration,
    writeTimeout: Duration
) : AsyncBaseClientSocket(coroutineScope, pool, readTimeout, writeTimeout), ClientToServerSocket {

    override suspend fun open(hostname: String?, port: UShort) {
        val socketAddress = InetSocketAddress(hostname?.asInetAddress(), port.toInt())
        val asyncSocket = asyncSocket()
        this.socket = asyncSocket
        asyncSocket.asyncSetOption(StandardSocketOptions.TCP_NODELAY, true)
        asyncSocket.asyncSetOption(StandardSocketOptions.SO_REUSEADDR, false)
        asyncSocket.asyncSetOption(StandardSocketOptions.SO_KEEPALIVE, true)
        asyncSocket.asyncSetOption(StandardSocketOptions.SO_RCVBUF, 100)
        asyncSocket.asyncSetOption(StandardSocketOptions.SO_SNDBUF, 100)
        asyncSocket.aConnect(socketAddress, tag)
        startWriteChannel()
    }

}


