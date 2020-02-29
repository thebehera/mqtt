package mqtt.transport.nio2.socket

import kotlinx.coroutines.*
import kotlinx.coroutines.flow.flow
import mqtt.transport.BufferPool
import mqtt.transport.ClientSocket
import mqtt.transport.ServerToClientSocket
import mqtt.transport.nio.socket.BaseServerSocket
import mqtt.transport.nio.socket.readStats
import mqtt.transport.nio2.util.aAccept
import mqtt.transport.nio2.util.aBind
import mqtt.transport.nio2.util.asyncSetOption
import mqtt.transport.nio2.util.openAsyncServerSocketChannel
import java.net.InetSocketAddress
import java.net.SocketAddress
import java.net.StandardSocketOptions
import java.nio.channels.AsynchronousCloseException
import java.nio.channels.AsynchronousServerSocketChannel
import java.nio.channels.AsynchronousSocketChannel
import java.nio.channels.NetworkChannel
import java.util.*
import kotlin.coroutines.resume
import kotlin.time.Duration
import kotlin.time.ExperimentalTime


@ExperimentalUnsignedTypes
@ExperimentalCoroutinesApi
@ExperimentalTime
class AsyncServerSocket2(
    parentScope: CoroutineScope,
    pool: BufferPool,
    readTimeout: Duration,
    writeTimeout: Duration
) : BaseServerSocket<AsynchronousServerSocketChannel>(parentScope, pool, readTimeout, writeTimeout) {
    override suspend fun accept(channel: AsynchronousServerSocketChannel) = channel.aAccept()

    override suspend fun bind(channel: AsynchronousServerSocketChannel, socketAddress: SocketAddress?) =
        channel.aBind(socketAddress)

    override suspend fun serverNetworkChannel() = openAsyncServerSocketChannel()

    override fun clientToServer(
        scope: CoroutineScope,
        networkChannel: NetworkChannel,
        pool: BufferPool,
        readTimeout: Duration,
        writeTimeout: Duration
    ) = AsyncServerToClientSocket(scope, networkChannel as AsynchronousSocketChannel, pool, readTimeout, writeTimeout)

}

@ExperimentalUnsignedTypes
@ExperimentalCoroutinesApi
@ExperimentalTime
class AsyncServerSocket(
    parentScope: CoroutineScope,
    val pool: BufferPool,
    val readTimeout: Duration,
    val writeTimeout: Duration
) : ServerToClientSocket {
    override val scope = parentScope + Job()
    private var server: AsynchronousServerSocketChannel? = null
    override val connections = TreeMap<UShort, AsyncServerToClientSocket>()

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
        val serverLocal = openAsyncServerSocketChannel()
        serverLocal.asyncSetOption(StandardSocketOptions.SO_REUSEADDR, false)
        server = serverLocal.aBind(socketAddress)
    }

    override suspend fun listen() = flow<ClientSocket> {
        try {
            while (isOpen()) {
                val asyncSocketChannel = server?.aAccept()
                asyncSocketChannel ?: continue
                val client = AsyncServerToClientSocket(scope, asyncSocketChannel, pool, readTimeout, writeTimeout)
                connections[(asyncSocketChannel.remoteAddress as InetSocketAddress).port.toUShort()] = client
                emit(client)
            }
        } catch (e: AsynchronousCloseException) {
            // we're done
        }
        this@AsyncServerSocket.close()
    }


    override suspend fun closeClient(port: UShort) {
        val connection = connections.remove(port)
        connection?.close()
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
