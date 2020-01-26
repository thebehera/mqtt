package mqtt.transport

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.suspendCancellableCoroutine
import mqtt.connection.ClientControlPacketTransport
import mqtt.transport.nio.AsyncClientTransport
import mqtt.wire.control.packet.IConnectionRequest
import java.nio.channels.AsynchronousChannelGroup
import java.nio.channels.AsynchronousSocketChannel
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.time.ExperimentalTime

@ExperimentalTime
actual suspend fun aMqttClient(
    scope: CoroutineScope,
    connectionRequest: IConnectionRequest,
    maxBufferSize: Int,
    group: Any?
): ClientControlPacketTransport {
    val socket = suspendCancellableCoroutine<AsynchronousSocketChannel> {
        try {
            it.resume(AsynchronousSocketChannel.open(group as? AsynchronousChannelGroup))
        } catch (e: Throwable) {
            it.resumeWithException(e)
        }
    }
    println("scope $scope, socket $socket con $connectionRequest maxBuffer $maxBufferSize")
    return AsyncClientTransport(scope, socket, connectionRequest, maxBufferSize)
}

