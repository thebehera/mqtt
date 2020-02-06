package mqtt.transport.nio2

import kotlinx.coroutines.*
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.consumeAsFlow
import kotlinx.coroutines.flow.flow
import mqtt.time.currentTimestampMs
import mqtt.transport.BufferPool
import mqtt.transport.ClientSocket
import mqtt.transport.ClientToServerSocket
import mqtt.transport.ServerToClientSocket
import mqtt.transport.nio2.util.*
import mqtt.transport.util.asInetAddress
import java.net.InetSocketAddress
import java.nio.ByteBuffer
import java.nio.channels.AsynchronousCloseException
import java.nio.channels.AsynchronousServerSocketChannel
import java.nio.channels.AsynchronousSocketChannel
import java.util.*
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.coroutines.resume
import kotlin.time.Duration
import kotlin.time.ExperimentalTime

@ExperimentalUnsignedTypes
@ExperimentalTime
abstract class AsyncBaseClientSocket(
    coroutineScope: CoroutineScope,
    override val pool: BufferPool<ByteBuffer>,
    override var readTimeout: Duration,
    override var writeTimeout: Duration
) : ClientSocket<ByteBuffer> {
    override val scope: CoroutineScope = coroutineScope + Job()
    protected var socket: AsynchronousSocketChannel? = null
    private val writeChannel = Channel<ByteBuffer>()
    private val isClosing = AtomicBoolean(false)

    override fun isOpen() = try {
        (socket?.isOpen ?: false) && !isClosing.get()
    } catch (e: Throwable) {
        false
    }

    override val incoming = flow {
        try {
            while (isOpen()) {
                val buffer = pool.borrow()
                socket!!.aRead(buffer, readTimeout)
                emit(buffer)
            }
        } finally {
            close()
        }
    }

    protected fun startWriteChannel(socket: AsynchronousSocketChannel) {
        scope.launch {
            try {
                writeChannel.consumeAsFlow().collect {
                    if (isActive) {
                        socket.aWrite(it, writeTimeout)
                    }
                }
            } finally {
                close()
            }
        }
    }


    override fun port(): UShort? {
        return socket?.assignedPort()
    }

    override suspend fun send(buffer: ByteBuffer) {
        writeChannel.send(buffer)
    }

    override suspend fun close() {
        writeChannel.close()
        socket?.aClose()
        socket = null
    }
}


@ExperimentalUnsignedTypes
@ExperimentalCoroutinesApi
@ExperimentalTime
class AsyncClientSocket(
    coroutineScope: CoroutineScope,
    pool: BufferPool<ByteBuffer>,
    readTimeout: Duration,
    writeTimeout: Duration
) : AsyncBaseClientSocket(coroutineScope, pool, readTimeout, writeTimeout), ClientToServerSocket<ByteBuffer> {

    override suspend fun open(hostname: String?, port: UShort) {
        val socketAddress = InetSocketAddress(hostname?.asInetAddress(), port.toInt())
//        println("${currentTimestampMs()} opened socket address $socketAddress")
        val asyncSocket = asyncSocket()
//        println("${currentTimestampMs()} async socket $socketAddress $asyncSocket")
        asyncSocket.aConnect(socketAddress)
//        println("${currentTimestampMs()} connected, start write channel $socketAddress $asyncSocket")
        startWriteChannel(asyncSocket)
//        println("${currentTimestampMs()} write channel started $socketAddress $asyncSocket")
        this.socket = asyncSocket
    }

}


@ExperimentalUnsignedTypes
@ExperimentalCoroutinesApi
@ExperimentalTime
class AsyncServerSocket(
    parentScope: CoroutineScope,
    val pool: BufferPool<ByteBuffer>,
    val readTimeout: Duration,
    val writeTimeout: Duration
) : ServerToClientSocket<ByteBuffer> {
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
        server = serverLocal.aBind(socketAddress)
    }

    override suspend fun listen() = flow<ClientSocket<ByteBuffer>> {
        try {
            while (isOpen()) {
//               println("${currentTimestampMs()} loop start $server")
                val asyncSocketChannel = server?.aAccept() ?: return@flow
//               println("${currentTimestampMs()} accepted socket channel $asyncSocketChannel")
//               asyncSocketChannel.setOption(SocketOptions.T)
//               println("${currentTimestampMs()} accepted client at ${asyncSocketChannel.assignedPort()}")
                try {
                    val client = AsyncServerToClientSocket(scope, asyncSocketChannel, pool, readTimeout, writeTimeout)
                    connections[client.asyncSocket.localAddress.toString()] = client
//                   println("${currentTimestampMs()} emit $client")
                    emit(client)
                } catch (e: Throwable) {
//                   println("${currentTimestampMs()} client connection closed $e")
                }
//               println("${currentTimestampMs()} loop end $server")
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

@ExperimentalUnsignedTypes
@ExperimentalTime
class AsyncServerToClientSocket(
    scope: CoroutineScope, val asyncSocket: AsynchronousSocketChannel,
    pool: BufferPool<ByteBuffer>,
    readTimeout: Duration,
    writeTimeout: Duration
) : AsyncBaseClientSocket(scope, pool, readTimeout, writeTimeout) {
    init {
        this.socket = asyncSocket
        startWriteChannel(asyncSocket)
    }
}
