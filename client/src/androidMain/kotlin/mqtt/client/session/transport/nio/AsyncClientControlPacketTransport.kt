package mqtt.client.session.transport.nio

import android.os.Build
import androidx.annotation.RequiresApi
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.ClosedSendChannelException
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.io.core.readByteBuffer
import mqtt.connection.ClientControlPacketTransport
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

    override suspend fun read(timeout: Duration) = socket.readPacket(packetBuffer, scope, timeout, protocolVersion)

    override suspend fun write(packet: ControlPacket, timeout: Duration): Int {
        val bytes = socket.writePacket(packet, timeout)
        socket.handleDisconnect(packet, outbound, inboxChannel)
        return bytes
    }


    override fun assignedPort(): UShort? {
        return socket.assignedPort()
    }

    override fun isOpen() = socket.isOpen


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

    private var pingTimerJob: Job? = null
    private var readJob: Job? = null
    private var writeJob: Job? = null

    override suspend fun open(port: UShort, host: String?): IConnectionAcknowledgment {
        val socketAddress = InetSocketAddress(address(host), port.toInt())
        socket.aConnect(socketAddress)
        write(connectionRequest, timeout)
        val packet = read(timeout)
        if (packet is IConnectionAcknowledgment) {
            readJob = startReadChannel()
            writeJob = startWriteChannel()
            pingTimerJob = startPingTimer()
        } else {
            throw IllegalStateException("Expected a Connection Acknowledgement, got $packet instead")
        }
        return packet
    }

    private fun startPingTimer() = scope.launch {
        while (isActive) {
            delayUntilPingInterval(connectionRequest.keepAliveTimeoutSeconds.toLong() * 1000L)
            try {
                outboundChannel.send(ping(connectionRequest.protocolVersion))
            } catch (e: ClosedSendChannelException) {
                return@launch
            }
        }
    }

    override suspend fun suspendClose() {
        try {
            pingTimerJob?.cancel()
            readJob?.cancel()
            writeJob?.cancel()
            super.suspendClose()
        } finally {
            socket.aClose()
        }
    }

}

suspend fun address(host: String?) = suspendCoroutine<InetAddress> {
    try {
        it.resume(InetAddress.getByName(host))
    } catch (e: Throwable) {
        it.resumeWithException(e)
    }
}


@ExperimentalTime
@RequiresApi(Build.VERSION_CODES.O)
suspend fun asyncClientTransport(
    scope: CoroutineScope,
    connectionRequest: IConnectionRequest,
    group: AsynchronousChannelGroup? = null,
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

//readConnectionRequest
@RequiresApi(Build.VERSION_CODES.O)
@ExperimentalTime
suspend fun AsynchronousSocketChannel.readConnectionRequest(
    packetBuffer: ByteBuffer,
    timeout: Duration
): IConnectionRequest? {
    aRead(packetBuffer, timeout.toLongMilliseconds())
    return packetBuffer.readConnectionRequest()
}

@RequiresApi(Build.VERSION_CODES.O)
@ExperimentalTime
suspend fun AsynchronousSocketChannel.readPacket(
    packetBuffer: ByteBuffer,
    scope: CoroutineScope,
    timeout: Duration,
    protocolVersion: Int
): ControlPacket {
    val pkt = aReadPacket(packetBuffer, scope, protocolVersion, timeout.toLongMilliseconds(), TimeUnit.MILLISECONDS)
    println("aReadPacket $pkt end")
    return pkt
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
    aClose()
    channelsToClose.forEach { it.close() }
    println("channels closed")
}

@RequiresApi(Build.VERSION_CODES.O)
fun AsynchronousSocketChannel.assignedPort(): UShort? {
    return try {
        (remoteAddress as? InetSocketAddress)?.port?.toUShort()
    } catch (e: Exception) {
        null
    }
}