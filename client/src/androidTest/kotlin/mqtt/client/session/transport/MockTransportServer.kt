package mqtt.client.session.transport

import android.os.Build
import androidx.annotation.RequiresApi
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.channelFlow
import mqtt.client.session.transport.nio.*
import mqtt.connection.ServerControlPacketTransport
import mqtt.wire.control.packet.IConnectionRequest
import java.net.InetSocketAddress
import java.nio.ByteBuffer
import java.nio.channels.AsynchronousChannelGroup
import java.nio.channels.AsynchronousServerSocketChannel
import java.nio.channels.AsynchronousSocketChannel
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.math.round
import kotlin.time.Duration
import kotlin.time.ExperimentalTime
import kotlin.time.seconds

@ExperimentalTime
class MockTransportServer(
    override val scope: CoroutineScope,
    val maxBufferSize: Int,
    val group: AsynchronousChannelGroup? = null
) : ServerControlPacketTransport {

    private lateinit var server: AsynchronousServerSocketChannel
    var localAddress: InetSocketAddress? = null
    private val readBuffer = ByteBuffer.allocateDirect(maxBufferSize)
    private val connections = HashSet<AsyncServerControlPacketTransport>()

    override suspend fun listen(
        port: UShort?,
        host: String,
        readTimeout: Duration
    ): Flow<AsyncServerControlPacketTransport> {
        val server = openAsyncServerSocketChannel(group)
        this.server = if (port != null) {
            server.aBind(InetSocketAddress(host, port.toInt()))
        } else {
            server.aBind(null)
        }
        println("Mock server bound to ${server.localAddress}")
        localAddress = server.localAddress as? InetSocketAddress
        return channelFlow {
            try {
                while (scope.isActive && server.isOpen) {
                    val connection = server.aAccept()
                    scope.launch {
                        val connectionRequest = connection.readConnectionRequest(readBuffer, 1.seconds)
                        if (connectionRequest != null) {
                            val transport =
                                AsyncServerControlPacketTransport(scope, connection, maxBufferSize, connectionRequest)
                            transport.openChannels()
                            connections.add(transport)
                            send(transport)
                        } else {
                            println("aClose")
                            connection.aClose()
                        }
                    }
                }
            } catch (e: Throwable) {
                println("server closed $e")
            } finally {
                close()
            }
        }
    }


    override fun close() {
        connections.forEach { runBlocking { it.socket.aClose() } }
        try {
            this.server.close()
        } catch (e: Throwable) {

        } finally {
            println("closed server")
        }
    }

}

@ExperimentalTime
@RequiresApi(Build.VERSION_CODES.O)
class AsyncServerControlPacketTransport(
    override val scope: CoroutineScope,
    socket: AsynchronousSocketChannel,
    maxBufferSize: Int,
    val connectionRequest: IConnectionRequest
) : JavaAsyncClientControlPacketTransport(
    scope,
    socket,
    4,
    maxBufferSize,
    connectionRequest.keepAliveTimeoutSeconds.toLong().seconds
) {

    fun openChannels() {
        startReadChannel()
        startWriteChannel()
        disconnectIfKeepAliveExpires()
    }

    private fun disconnectIfKeepAliveExpires() = scope.launch {
        val timeout = round(connectionRequest.keepAliveTimeoutSeconds.toFloat() * 1.5f).toLong()
        do {
            delayUntilPingInterval(timeout)
        } while (isActive && !isClosing && nextDelay(timeout) >= 0)
        println("closing $socket because of nextDelay timeout")
        suspendClose()
    }

    override suspend fun suspendClose() {
        isClosing = true
        try {
            super.suspendClose()
        } catch (e: Throwable) {

        }
    }

    override fun close() {
        super.close()
        socket.close()
    }
}


suspend fun openAsyncServerSocketChannel(group: AsynchronousChannelGroup? = null): AsynchronousServerSocketChannel =
    suspendCancellableCoroutine { continuation ->
        try {
            continuation.resume(AsynchronousServerSocketChannel.open(group))
        } catch (e: Exception) {
            continuation.resumeWithException(e)
        }
    }