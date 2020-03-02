package mqtt.socket.nio

import mqtt.socket.ClientToServerSocket
import mqtt.socket.SocketOptions
import mqtt.socket.nio.util.*
import java.net.InetSocketAddress
import kotlin.time.Duration
import kotlin.time.ExperimentalTime
import kotlin.time.MonoClock

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
            println("\"${MonoClock.markNow()} FAILED TO CONNECT CLIENT client ${(socketChannel.remoteAddress as? InetSocketAddress)?.port} $socketChannel")
        }
        return socketChannel.asyncSetOptions(socketOptions)
    }
}

