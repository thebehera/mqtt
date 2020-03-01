package mqtt.transport.nio2.socket

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
@ExperimentalTime
class AsyncClientSocket : AsyncBaseClientSocket(), ClientToServerSocket {

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
        asyncSocket.aConnect(socketAddress)
        return options
    }

}


