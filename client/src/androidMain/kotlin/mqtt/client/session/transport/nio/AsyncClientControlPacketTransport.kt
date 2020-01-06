package mqtt.client.session.transport.nio

import android.os.Build
import androidx.annotation.RequiresApi
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.sendBlocking
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.io.core.readByteBuffer
import mqtt.connection.ClientControlPacketTransport
import mqtt.time.currentTimestampMs
import mqtt.wire.control.packet.ControlPacket
import mqtt.wire.control.packet.IConnectionAcknowledgment
import mqtt.wire.control.packet.IConnectionRequest
import mqtt.wire.control.packet.IDisconnectNotification
import java.net.InetAddress
import java.net.InetSocketAddress
import java.net.StandardSocketOptions
import java.nio.ByteBuffer
import java.nio.channels.AsynchronousSocketChannel
import java.util.concurrent.TimeUnit
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.coroutines.suspendCoroutine
import kotlin.math.min
import kotlin.math.roundToLong
import kotlin.time.Duration
import kotlin.time.ExperimentalTime
import kotlin.time.seconds

@ExperimentalTime
@RequiresApi(Build.VERSION_CODES.O)
abstract class JavaAsyncClientControlPacketTransport(
    override val scope: CoroutineScope,
    open val socket: AsynchronousSocketChannel,
    protocolVersion: Int,
    timeout: Duration,
    override val maxBufferSize: Int
) : AbstractClientControlPacketTransport(scope, protocolVersion, timeout, maxBufferSize) {
    protected val packetBuffer: ByteBuffer by lazy {
        ByteBuffer.allocateDirect(
            min(
                maxBufferSize,
                socket.getOption(StandardSocketOptions.SO_RCVBUF)
            )
        )
    }

    override suspend fun read(timeout: Duration) = socket.readPacket(packetBuffer, timeout, protocolVersion)

    override suspend fun write(packet: ControlPacket, timeout: Duration): Int {
        val bytes = socket.writePacket(packet, timeout)
        socket.handleDisconnect(packet, outbound)
        return bytes
    }


    override fun assignedPort(): UShort? {
        return socket.assignedPort()
    }

    override fun close() {
        outboundChannel.sendBlocking(disconnect(protocolVersion))
        super.close()
    }

}

@ExperimentalTime
@RequiresApi(Build.VERSION_CODES.O)
class AsyncClientControlPacketTransport(
    override val scope: CoroutineScope,
    override val socket: AsynchronousSocketChannel,
    override val connectionRequest: IConnectionRequest,
    override val maxBufferSize: Int
) : JavaAsyncClientControlPacketTransport(
    scope, socket, connectionRequest.protocolVersion,
    connectionRequest.keepAliveTimeoutSeconds.toLong().seconds, maxBufferSize
), ClientControlPacketTransport {

    override suspend fun open(port: UShort, host: String?): IConnectionAcknowledgment {
        socket.suspendConnectSocket(InetSocketAddress(address(host), port.toInt()))
        write(connectionRequest, timeout)
        val packet = read(timeout)
        if (packet is IConnectionAcknowledgment) {
            startReadChannel()
            startWriteChannel()
            startPingTimer()
        } else {
            throw IllegalStateException("Expected a Connection Acknowledgement, got $packet instead")
        }
        return packet
    }

    private fun startPingTimer() = scope.launch {
        delayUntilPingInterval()
        while (!isClosing && isActive && assignedPort() != null) {
            outboundChannel.send(ping(connectionRequest.protocolVersion))
            delayUntilPingInterval()
        }
    }

    private suspend fun delayUntilPingInterval() {
        val keepAliveMs = connectionRequest.keepAliveTimeoutSeconds.toLong() * 1000L
        val nextMessageTime = lastMessageReadAt + keepAliveMs
        val time = currentTimestampMs()
        var deltaTime = nextMessageTime - time
        if (deltaTime < 0) {
            deltaTime = keepAliveMs
        }
        delay(deltaTime)
    }

}

suspend fun address(host: String?) = suspendCoroutine<InetAddress> {
    try {
        it.resume(InetAddress.getByName(host))
    } catch (e: Exception) {
        it.resumeWithException(e)
    }
}

@RequiresApi(Build.VERSION_CODES.O)
suspend fun AsynchronousSocketChannel.suspendConnectSocket(inetSocketAddress: InetSocketAddress) {
    suspendCoroutine<Void?> { continuation ->
        try {
            connect(inetSocketAddress, continuation, ConnectCompletionHandler)
        } catch (e: Exception) {
            continuation.resumeWithException(e)
        }
    }
}

@ExperimentalTime
@RequiresApi(Build.VERSION_CODES.O)
suspend fun asyncClientTransport(
    scope: CoroutineScope,
    connectionRequest: IConnectionRequest,
    maxBufferSize: Int = 12_000
): ClientControlPacketTransport {
    val socket = suspendCoroutine<AsynchronousSocketChannel> {
        try {
            it.resume(AsynchronousSocketChannel.open())
        } catch (e: Exception) {
            it.resumeWithException(e)
        }
    }
    return AsyncClientControlPacketTransport(scope, socket, connectionRequest, maxBufferSize)
}


@RequiresApi(Build.VERSION_CODES.O)
@ExperimentalTime
suspend fun AsynchronousSocketChannel.readPacket(
    packetBuffer: ByteBuffer,
    timeout: Duration,
    protocolVersion: Int
): ControlPacket {
    val metadata = suspendCoroutine<FixedHeaderMetadata> { continuation ->
        read(
            packetBuffer, timeout.inMilliseconds.roundToLong(), TimeUnit.MILLISECONDS,
            continuation, FixedHeaderCompletionHandler(packetBuffer)
        )
    }
    return if (metadata.remainingLength.toLong() < packetBuffer.remaining()) { // we already read the entire message in the buffer
        packetBuffer.read(protocolVersion)
    } else {
        suspendCoroutine {
            throw UnsupportedOperationException("TODO: WIP to read buffers larger than whats larger than max buffer")
        }
    }
}

@RequiresApi(Build.VERSION_CODES.O)
@UseExperimental(ExperimentalTime::class)
suspend fun AsynchronousSocketChannel.writePacket(packet: ControlPacket, timeout: Duration): Int {
    val bytes = suspendCoroutine<Int> { continuation ->
        try {
            write(
                packet.serialize().readByteBuffer(), timeout.inMilliseconds.roundToLong(),
                TimeUnit.MILLISECONDS, continuation, WriteCompletionHandler
            )
        } catch (e: Exception) {
            continuation.resumeWithException(RuntimeException("failed to write $packet", e))
        }
    }
    return bytes
}

@RequiresApi(Build.VERSION_CODES.O)
suspend fun AsynchronousSocketChannel.handleDisconnect(packet: ControlPacket, channelToClose: Channel<ControlPacket>) {
    if (packet is IDisconnectNotification) {
        suspendCoroutine<Void?> { continuation ->
            try {
                close()
                channelToClose.close()
                continuation.resume(null)
            } catch (e: Exception) {
                continuation.resumeWithException(RuntimeException("failed to close after $packet", e))
            }
        }
    }
}

@RequiresApi(Build.VERSION_CODES.O)
fun AsynchronousSocketChannel.assignedPort(): UShort? {
    return try {
        (remoteAddress as? InetSocketAddress)?.port?.toUShort()
    } catch (e: Exception) {
        null
    }
}