package mqtt.transport.nio2.socket

import kotlinx.coroutines.*
import kotlinx.coroutines.flow.flow
import mqtt.time.currentTimestampMs
import mqtt.transport.BufferPool
import mqtt.transport.ClientSocket
import mqtt.transport.ServerToClientSocket
import mqtt.transport.nio.socket.BaseServerSocket
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
    val connections = TreeMap<String, AsyncServerToClientSocket>()

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
                println("${currentTimestampMs()}      server accepted $asyncSocketChannel}")
                asyncSocketChannel ?: continue
                val client = AsyncServerToClientSocket(scope, asyncSocketChannel, pool, readTimeout, writeTimeout)
                connections[asyncSocketChannel.localAddress.toString()] = client
                emit(client)
            }
        } catch (e: AsynchronousCloseException) {
            // we're done
        }
        println("${currentTimestampMs()} done listening")
        this@AsyncServerSocket.close()
    }

    override suspend fun close() {
        if (server?.isOpen != true && connections.isNotEmpty()) {
            return
        }
        println("${currentTimestampMs()} closing ${connections.size} connections")
        connections.values.map {
            scope.launch {
                if (isActive) {
                    println("${currentTimestampMs()} closing client socket $it")
                    it.close()
                }
            }
        }.joinAll()
        connections.clear()
        println("${currentTimestampMs()} closing server client socket")
        suspendCancellableCoroutine<Unit> {
            try {
                server?.close()
            } catch (e: Throwable) {

            } finally {
                it.resume(Unit)
            }
        }
        println("${currentTimestampMs()} server closed")
    }
}
