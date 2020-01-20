package mqtt.client.session.transport

import android.os.Build
import androidx.annotation.RequiresApi
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import mqtt.client.session.transport.nio.*
import mqtt.connection.ControlPacketTransport
import mqtt.connection.ServerControlPacketTransport
import mqtt.wire.control.packet.ControlPacket
import mqtt.wire.control.packet.IConnectionRequest
import java.net.InetSocketAddress
import java.nio.ByteBuffer
import java.nio.channels.AsynchronousServerSocketChannel
import java.nio.channels.AsynchronousSocketChannel
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.time.Duration
import kotlin.time.ExperimentalTime
import kotlin.time.seconds

@ExperimentalTime
class MockTransportServer(
    override val scope: CoroutineScope,
    val maxBufferSize: Int,
    val timeout: Duration
) : ServerControlPacketTransport {

    private lateinit var server: AsynchronousServerSocketChannel
    var localAddress: InetSocketAddress? = null
    private val outgoing = Channel<ControlPacket>()
    private val readBuffer = ByteBuffer.allocateDirect(maxBufferSize)
    suspend fun queuePacket(packet: ControlPacket) {
        outgoing.send(packet)
    }

    override suspend fun listen(port: UShort?, host: String): Flow<ControlPacketTransport> {
        val server = openAsyncServerSocketChannel()
        this.server = if (port != null) {
            server.aBind(InetSocketAddress(host, port.toInt()))
        } else {
            server.aBind(null)
        }
        localAddress = server.localAddress as? InetSocketAddress
        return flow {
            while (scope.isActive) {
                val connection = server.aAccept()
                scope.launch {
                    val connectionRequest =
                        connection.readPacket(readBuffer, this, 1.seconds, 4)
                    if (connectionRequest is IConnectionRequest) {
                        val transport =
                            AsyncServerControlPacketTransport(scope, connection, maxBufferSize, connectionRequest)
                        emit(transport)
                        // start connection in a seperate method
                        //val incomingPacket = connection.readPacket(readBuffer, connectionRequest.keepAliveTimeoutSeconds.toLong().seconds, connectionRequest.protocolVersion)
                    } else {
                        connection.aClose()
                    }
                }
            }
        }
    }


    override fun close() {

    }

}

@ExperimentalTime
@RequiresApi(Build.VERSION_CODES.O)
class AsyncServerControlPacketTransport(
    override val scope: CoroutineScope,
    socket: AsynchronousSocketChannel,
    maxBufferSize: Int,
    connectionRequest: IConnectionRequest
) : JavaAsyncClientControlPacketTransport(
    scope,
    socket,
    4,
    maxBufferSize,
    connectionRequest.keepAliveTimeoutSeconds.toLong().seconds
) {


    override fun close() {
        socket.close()
    }
}


suspend fun openAsyncServerSocketChannel(): AsynchronousServerSocketChannel =
    suspendCancellableCoroutine { continuation ->
        try {
            continuation.resume(AsynchronousServerSocketChannel.open())
        } catch (e: Exception) {
            continuation.resumeWithException(e)
        }
    }