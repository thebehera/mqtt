package mqtt.transport.nio2

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import mqtt.wire.control.packet.IConnectionAcknowledgment
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
    override val connectionRequest: IConnectionRequest
) : AsyncBaseClientTransport(
    scope,
    socket,
    connectionRequest,
    maxBufferSize,
    connectionRequest.keepAliveTimeoutSeconds.toLong().seconds
) {
    init {
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

    override suspend fun open(port: UShort, host: String?): IConnectionAcknowledgment {
        throw UnsupportedOperationException("Can't open a port directly")
    }
}