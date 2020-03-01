package mqtt.transport.nio.socket

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.suspendCancellableCoroutine
import mqtt.transport.BufferPool
import mqtt.transport.ClientSocket
import mqtt.transport.ServerToClientSocket
import mqtt.transport.SocketOptions
import mqtt.transport.nio.socket.util.asyncSetOptions
import java.net.InetSocketAddress
import java.net.SocketAddress
import java.nio.channels.AsynchronousCloseException
import java.nio.channels.AsynchronousSocketChannel
import java.nio.channels.NetworkChannel
import java.util.*
import kotlin.coroutines.resume
import kotlin.time.ExperimentalTime

@ExperimentalUnsignedTypes
@ExperimentalCoroutinesApi
@ExperimentalTime
abstract class BaseServerSocket<S : NetworkChannel>(val pool: BufferPool) : ServerToClientSocket {
    private var server: S? = null
    override val connections = TreeMap<UShort, ClientSocket>()

    override fun port() = (server?.localAddress as? InetSocketAddress)?.port?.toUShort()

    fun isOpen() = try {
        port() != null && server?.isOpen ?: false
    } catch (e: Throwable) {
        false
    }

    override suspend fun bind(port: UShort?, host: String?, socketOptions: SocketOptions?): SocketOptions {
        val socketAddress = if (port != null) {
            InetSocketAddress(host, port.toInt())
        } else {
            null
        }
        val serverLocal = serverNetworkChannel()
        val options = serverLocal.asyncSetOptions(socketOptions)
        server = bind(serverLocal, socketAddress)
        return options
    }

    abstract suspend fun accept(channel: S): NetworkChannel?

    abstract suspend fun bind(channel: S, socketAddress: SocketAddress?): S?
    abstract suspend fun serverNetworkChannel(): S

    abstract fun clientToServer(networkChannel: NetworkChannel): ByteBufferClientSocket<AsynchronousSocketChannel>

    override suspend fun listen() = flow<ClientSocket> {
        try {
            while (isOpen()) {
                val asyncSocketChannel = accept(server!!)
                asyncSocketChannel ?: continue
                val client = clientToServer(asyncSocketChannel)
                connections[client.remotePort()!!] = client
                emit(client)
            }
        } catch (e: AsynchronousCloseException) {
            // we're done
        }
        this@BaseServerSocket.close()
    }

    override suspend fun closeClient(port: UShort) {
        connections.remove(port)?.close()
    }

    override fun getStats() = readStats(port()!!, "CLOSE_WAIT")

    override suspend fun close() {
        if (server?.isOpen != true && connections.isNotEmpty()) {
            return
        }
        connections.values.forEach { it.close() }
        connections.clear()
        suspendCancellableCoroutine<Unit> {
            try {
                server?.close()
            } catch (e: Throwable) {

            } finally {
                it.resume(Unit)
            }
        }
    }
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