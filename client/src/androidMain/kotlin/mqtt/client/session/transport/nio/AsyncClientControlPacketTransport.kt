package mqtt.client.session.transport.nio

import android.os.Build
import androidx.annotation.RequiresApi
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.channels.Channel
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
import java.nio.channels.AsynchronousChannelGroup
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
open class JavaAsyncClientControlPacketTransport(
    override val scope: CoroutineScope,
    open val socket: AsynchronousSocketChannel,
    protocolVersion: Int,
    override val maxBufferSize: Int,
    timeout: Duration, timeoutMultiplier: Double = 1.5
) : AbstractClientControlPacketTransport(scope, protocolVersion, timeout, timeoutMultiplier, maxBufferSize) {
    private val packetBuffer: ByteBuffer by lazy {
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
        socket.handleDisconnect(packet, outbound, inboxChannel)
        return bytes
    }


    override fun assignedPort(): UShort? {
        return socket.assignedPort()
    }

    override fun isOpen() = socket.isOpen

    override fun close() {
        if (outboundChannel.isClosedForSend) {
            return
        }
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
    scope, socket, connectionRequest.protocolVersion, maxBufferSize,
    connectionRequest.keepAliveTimeoutSeconds.toLong().seconds
), ClientControlPacketTransport {

    override suspend fun open(port: UShort, host: String?): IConnectionAcknowledgment {
        val socketAddress = InetSocketAddress(address(host), port.toInt())
        socket.aConnect(socketAddress)
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


@ExperimentalTime
@RequiresApi(Build.VERSION_CODES.O)
suspend fun asyncClientTransport(
    scope: CoroutineScope,
    connectionRequest: IConnectionRequest,
    group: AsynchronousChannelGroup,
    maxBufferSize: Int = 12_000
): ClientControlPacketTransport {
    val socket = suspendCoroutine<AsynchronousSocketChannel> {
        try {
            it.resume(AsynchronousSocketChannel.open(group))
        } catch (e: Throwable) {
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
    aRead(packetBuffer, timeout.inMilliseconds.roundToLong(), TimeUnit.MILLISECONDS)
    packetBuffer.flip()
    val position = packetBuffer.position()
    val metadata = FixedHeaderMetadata(packetBuffer.get().toUByte(), packetBuffer.decodeVariableByteInteger())
    packetBuffer.position(position)
    return if (metadata.remainingLength.toLong() < packetBuffer.remaining()) { // we already read the entire message in the buffer
        packetBuffer.read(protocolVersion)
    } else {
        throw UnsupportedOperationException("TODO: WIP to read buffers larger than whats larger than max buffer")
    }
}

data class FixedHeaderMetadata(val firstByte: UByte, val remainingLength: UInt)

@RequiresApi(Build.VERSION_CODES.O)
@UseExperimental(ExperimentalTime::class)
suspend fun AsynchronousSocketChannel.writePacket(packet: ControlPacket, timeout: Duration): Int {
    val bytes = aWrite(
        packet.serialize().readByteBuffer(direct = true),
        timeout.inMilliseconds.roundToLong(),
        TimeUnit.MILLISECONDS
    )
//    println("wrote $packet")
    return bytes
}


@RequiresApi(Build.VERSION_CODES.O)
suspend fun AsynchronousSocketChannel.handleDisconnect(
    packet: ControlPacket,
    vararg channelsToClose: Channel<ControlPacket>
) {
    if (packet !is IDisconnectNotification) {
        return
    }
    channelsToClose.forEach { it.close() }
    aClose()
}

@RequiresApi(Build.VERSION_CODES.O)
fun AsynchronousSocketChannel.assignedPort(): UShort? {
    return try {
        (remoteAddress as? InetSocketAddress)?.port?.toUShort()
    } catch (e: Exception) {
        null
    }
}