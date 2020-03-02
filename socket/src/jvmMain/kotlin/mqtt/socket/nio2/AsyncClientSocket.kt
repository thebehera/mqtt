package mqtt.socket.nio2

import mqtt.socket.ClientToServerSocket
import mqtt.socket.SocketOptions
import mqtt.socket.nio.util.asInetAddress
import mqtt.socket.nio.util.asyncSetOptions
import mqtt.socket.nio2.util.aConnect
import mqtt.socket.nio2.util.asyncSocket
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


