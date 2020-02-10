package mqtt.transport.nio.socket

import kotlinx.coroutines.*
import kotlinx.coroutines.flow.flow
import mqtt.time.currentTimestampMs
import mqtt.transport.BufferPool
import mqtt.transport.ClientSocket
import mqtt.transport.ServerToClientSocket
import mqtt.transport.nio.socket.util.asyncSetOption
import java.net.InetSocketAddress
import java.net.SocketAddress
import java.net.StandardSocketOptions
import java.nio.ByteBuffer
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
    parentScope: CoroutineScope,
    val pool: BufferPool<ByteBuffer>,
    val readTimeout: Duration,
    val writeTimeout: Duration
) : ServerToClientSocket<ByteBuffer> {
    override val scope = parentScope + Job()
    private var server: S? = null
    val connections = TreeMap<String, ByteBufferClientSocket<AsynchronousSocketChannel>>()

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
        pool: BufferPool<ByteBuffer>, readTimeout: Duration, writeTimeout: Duration
    ): ByteBufferClientSocket<AsynchronousSocketChannel>

    override suspend fun listen() = flow<ClientSocket<ByteBuffer>> {
        try {
            while (isOpen()) {
                val asyncSocketChannel = accept(server!!)
                println("${currentTimestampMs()}      server accepted $asyncSocketChannel}")
                asyncSocketChannel ?: continue
                val client = clientToServer(scope, asyncSocketChannel, pool, readTimeout, writeTimeout)
                connections[asyncSocketChannel.localAddress.toString()] = client
                emit(client)
            }
        } catch (e: AsynchronousCloseException) {
            // we're done
        }
        println("${currentTimestampMs()} done listening")
        this@BaseServerSocket.close()
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
