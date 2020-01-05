package mqtt.client.session.transport

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.isActive
import mqtt.client.session.transport.nio.JavaAsyncClientControlPacketTransport
import mqtt.connection.ControlPacketTransport
import mqtt.connection.ServerControlPacketTransport
import java.net.InetSocketAddress
import java.nio.channels.AsynchronousServerSocketChannel
import java.nio.channels.AsynchronousSocketChannel
import java.nio.channels.CompletionHandler
import kotlin.coroutines.Continuation
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.coroutines.suspendCoroutine
import kotlin.time.Duration
import kotlin.time.ExperimentalTime

@ExperimentalTime
class MockTransportServer(
    override val scope: CoroutineScope
) : ServerControlPacketTransport {

    private lateinit var server: AsynchronousServerSocketChannel
    var localAddress: InetSocketAddress? = null

    override suspend fun listen(port: UShort?, host: String): Flow<ControlPacketTransport> {
        val server = openAsyncServerSocketChannel()
        this.server = if (port != null) {
            server.suspendBind(InetSocketAddress(host, port.toInt()))
        } else {
            server.suspendBind(null)
        }
        localAddress = server.localAddress as? InetSocketAddress
        val asyncSocketFlow = flow {
            while (scope.isActive) {
                emit(server.suspendAccept())
            }
        }
        return flow {

        }
    }


    override fun close() {

    }

}

@ExperimentalTime
class AsyncServerClient(
    scope: CoroutineScope, socket: AsynchronousSocketChannel,
    protocolVersion: Int, timeout: Duration, maxBufferSize: Int
) :
    JavaAsyncClientControlPacketTransport(scope, socket, protocolVersion, timeout, maxBufferSize)

suspend fun openAsyncServerSocketChannel(): AsynchronousServerSocketChannel = suspendCoroutine { continuation ->
    try {
        continuation.resume(AsynchronousServerSocketChannel.open())
    } catch (e: Exception) {
        continuation.resumeWithException(e)
    }
}

suspend fun AsynchronousServerSocketChannel.suspendBind(address: InetSocketAddress? = null, backlog: Int = 0)
        : AsynchronousServerSocketChannel = suspendCoroutine { continuation ->
    try {
        continuation.resume(bind(address, backlog))
    } catch (e: Exception) {
        continuation.resumeWithException(e)
    }
}

object ServerAcceptCompletionHandler :
    CompletionHandler<AsynchronousSocketChannel, Continuation<AsynchronousSocketChannel>> {
    override fun completed(result: AsynchronousSocketChannel, attachment: Continuation<AsynchronousSocketChannel>) =
        attachment.resume(result)

    override fun failed(exc: Throwable, attachment: Continuation<AsynchronousSocketChannel>) =
        attachment.resumeWithException(exc)
}

suspend fun AsynchronousServerSocketChannel.suspendAccept(): AsynchronousSocketChannel =
    suspendCoroutine { continuation ->
        accept(continuation, ServerAcceptCompletionHandler)
    }