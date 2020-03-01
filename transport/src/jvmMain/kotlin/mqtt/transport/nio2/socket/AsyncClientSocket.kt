package mqtt.transport.nio2.socket

import kotlinx.coroutines.ExperimentalCoroutinesApi
import mqtt.transport.BufferPool
import mqtt.transport.ClientToServerSocket
import mqtt.transport.SocketOptions
import mqtt.transport.nio.socket.util.asyncSetOptions
import mqtt.transport.nio2.util.aConnect
import mqtt.transport.nio2.util.asyncSocket
import mqtt.transport.util.asInetAddress
import java.net.InetSocketAddress
import kotlin.time.Duration
import kotlin.time.ExperimentalTime

@ExperimentalUnsignedTypes
@ExperimentalCoroutinesApi
@ExperimentalTime
class AsyncClientSocket(pool: BufferPool) : AsyncBaseClientSocket(pool), ClientToServerSocket {

    override suspend fun open(
        timeout: Duration,
        port: UShort,
        hostname: String?,
        socketOptions: SocketOptions?
    ): SocketOptions {
        val socketAddress = InetSocketAddress(hostname?.asInetAddress(), port.toInt())
        val asyncSocket = asyncSocket()
        this.socket = asyncSocket
        val options = asyncSocket.asyncSetOptions(socketOptions)
        asyncSocket.aConnect(socketAddress, tag)
        return options
    }

}


