package mqtt.transport.nio.socket

import kotlinx.coroutines.*
import kotlinx.coroutines.flow.flow
import mqtt.transport.BufferPool
import mqtt.transport.ClientSocket
import mqtt.transport.ServerToClientSocket
import mqtt.transport.nio.socket.util.asyncSetOption
import java.net.InetSocketAddress
import java.net.SocketAddress
import java.net.StandardSocketOptions
import java.nio.channels.AsynchronousCloseException
import java.nio.channels.AsynchronousSocketChannel
import java.nio.channels.NetworkChannel
import java.util.*
import kotlin.coroutines.resume
import kotlin.time.Duration
import kotlin.time.ExperimentalTime

@ExperimentalUnsignedTypes
@ExperimentalCoroutinesApi
@ExperimentalTime
abstract class BaseServerSocket<S : NetworkChannel>(
    override val scope: CoroutineScope,
    val pool: BufferPool,
    val readTimeout: Duration,
    val writeTimeout: Duration
) : ServerToClientSocket {
    private var server: S? = null
    override val connections = TreeMap<UShort, ClientSocket>()

    override fun port() = (server?.localAddress as? InetSocketAddress)?.port?.toUShort()

    fun isOpen() = try {
        port() != null && server?.isOpen ?: false && scope.isActive
    } catch (e: Throwable) {
        false
    }

    override suspend fun bind(port: UShort?, host: String?) {
        val socketAddress = if (port != null) {
            InetSocketAddress(host, port.toInt())
        } else {
            null
        }
        val serverLocal = serverNetworkChannel()
        serverLocal.asyncSetOption(StandardSocketOptions.SO_REUSEADDR, false)
        server = bind(serverLocal, socketAddress)
    }

    abstract suspend fun accept(channel: S): NetworkChannel?

    abstract suspend fun bind(channel: S, socketAddress: SocketAddress?): S?
    abstract suspend fun serverNetworkChannel(): S

    abstract fun clientToServer(
        scope: CoroutineScope, networkChannel: NetworkChannel,
        pool: BufferPool, readTimeout: Duration, writeTimeout: Duration
    ): ByteBufferClientSocket<AsynchronousSocketChannel>

    override suspend fun listen() = flow<ClientSocket> {
        try {
            while (isOpen()) {
                val asyncSocketChannel = accept(server!!)
                asyncSocketChannel ?: continue
                val client = clientToServer(scope, asyncSocketChannel, pool, readTimeout, writeTimeout)
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
        connections.values.map {
            scope.launch {
                if (isActive) {
                    it.close()
                }
            }
        }.joinAll()
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