package mqtt.socket.nio

import mqtt.socket.ServerSocket
import mqtt.socket.SocketOptions
import mqtt.socket.nio.util.aClose
import mqtt.socket.nio.util.asyncSetOptions
import java.net.InetSocketAddress
import java.net.SocketAddress
import java.nio.channels.NetworkChannel
import kotlin.time.ExperimentalTime

@ExperimentalUnsignedTypes
@ExperimentalTime
abstract class BaseServerSocket<S : NetworkChannel> : ServerSocket {
    protected var server: S? = null

    override fun port() = (server?.localAddress as? InetSocketAddress)?.port?.toUShort()

    override fun isOpen() = try {
        port() != null && server?.isOpen ?: false
    } catch (e: Throwable) {
        false
    }

    override suspend fun bind(
        port: UShort?,
        host: String?,
        socketOptions: SocketOptions?,
        backlog: UInt
    ): SocketOptions {
        val socketAddress = if (port != null) {
            InetSocketAddress(host ?: "localhost", port.toInt())
        } else {
            null
        }
        val serverLocal = serverNetworkChannel()
        val options = serverLocal.asyncSetOptions(socketOptions)
        server = bind(serverLocal, socketAddress, backlog)
        return options
    }

    abstract suspend fun bind(channel: S, socketAddress: SocketAddress?, backlog: UInt): S
    abstract suspend fun serverNetworkChannel(): S

    override suspend fun close() {
        server?.aClose()
    }
}