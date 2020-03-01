package mqtt.transport.nio.socket

import kotlinx.coroutines.flow.flow
import mqtt.transport.ClientSocket
import mqtt.transport.Server
import mqtt.transport.ServerSocket
import mqtt.transport.SocketOptions
import mqtt.transport.nio.socket.util.aClose
import mqtt.transport.nio.socket.util.asyncSetOptions
import java.net.InetSocketAddress
import java.net.SocketAddress
import java.nio.channels.ClosedChannelException
import java.nio.channels.NetworkChannel
import java.util.*
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
            InetSocketAddress(host, port.toInt())
        } else {
            null
        }
        val serverLocal = serverNetworkChannel()
        val options = serverLocal.asyncSetOptions(socketOptions)
        server = bind(serverLocal, socketAddress, backlog)
        return options
    }

    abstract suspend fun bind(channel: S, socketAddress: SocketAddress?, backlog: UInt): S?
    abstract suspend fun serverNetworkChannel(): S

    override suspend fun close() {
        server?.aClose()
    }
}


@ExperimentalTime
@ExperimentalUnsignedTypes
class JvmServer(val serverSocket: ServerSocket) : Server {
    override val connections = TreeMap<UShort, ClientSocket>()
    override suspend fun listen() = flow {
        try {
            while (serverSocket.isOpen()) {
                val client = serverSocket.accept()
                connections[client.remotePort()!!] = client
                emit(client)
            }
        } catch (e: ClosedChannelException) {
            // we're done
        }
        serverSocket.close()
    }

    override suspend fun closeClient(port: UShort) {
        connections.remove(port)?.close()
    }

    suspend fun close() {
        if (!serverSocket.isOpen() && connections.isNotEmpty()) {
            return
        }
        connections.values.forEach { it.close() }
        connections.clear()
    }

    override fun getStats() = readStats(serverSocket.port()!!, "CLOSE_WAIT")
}

fun readStats(port: UShort, contains: String): List<String> {
    val process = ProcessBuilder()
        .command("lsof", "-iTCP:${port}", "-sTCP:$contains", "-l", "-n")
        .redirectErrorStream(true)
        .start()
    try {
        process.inputStream.use { stream ->
            return String(stream.readBytes()).split(System.lineSeparator()).filter { it.isNotBlank() }
                .filter { it.contains(contains) }
        }
    } finally {
        process.destroy()
    }
}