package mqtt.transport.nio.socket

import mqtt.time.currentTimestampMs
import mqtt.transport.ClientToServerSocket
import mqtt.transport.SocketOptions
import mqtt.transport.nio.socket.util.aConfigureBlocking
import mqtt.transport.nio.socket.util.asyncSetOptions
import mqtt.transport.nio.socket.util.connect
import mqtt.transport.nio.socket.util.openSocketChannel
import mqtt.transport.util.asInetAddress
import java.net.InetSocketAddress
import kotlin.time.Duration
import kotlin.time.ExperimentalTime

@ExperimentalUnsignedTypes
@ExperimentalTime
class NioClientSocket(
    blocking: Boolean = true
) : BaseClientSocket(blocking), ClientToServerSocket {
    override suspend fun open(
        timeout: Duration,
        port: UShort,
        hostname: String?,
        socketOptions: SocketOptions?
    ): SocketOptions {
        val socketAddress = InetSocketAddress(hostname?.asInetAddress(), port.toInt())
        val socketChannel = openSocketChannel()
        socketChannel.aConfigureBlocking(blocking)
        this.socket = socketChannel
        if (!socketChannel.connect(socketAddress, selector, timeout)) {
            println("\"${currentTimestampMs()} FAILED TO CONNECT CLIENT client ${(socketChannel.remoteAddress as? InetSocketAddress)?.port} $socketChannel")
        }
        return socketChannel.asyncSetOptions(socketOptions)
    }
}


