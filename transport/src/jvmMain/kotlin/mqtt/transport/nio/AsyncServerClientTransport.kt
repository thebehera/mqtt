package mqtt.transport.nio

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import mqtt.wire.control.packet.IConnectionRequest
import java.nio.channels.AsynchronousSocketChannel
import kotlin.math.round
import kotlin.time.ExperimentalTime
import kotlin.time.seconds

@ExperimentalTime
internal class AsyncServerClientTransport(
    override val scope: CoroutineScope,
    socket: AsynchronousSocketChannel,
    maxBufferSize: Int,
    val connectionReq: IConnectionRequest
) : AsyncBaseClientTransport(
    scope,
    socket,
    connectionReq,
    maxBufferSize,
    connectionReq.keepAliveTimeoutSeconds.toLong().seconds
) {
    internal fun openChannels() {
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