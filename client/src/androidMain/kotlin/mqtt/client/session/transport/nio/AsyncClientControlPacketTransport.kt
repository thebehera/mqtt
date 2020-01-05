package mqtt.client.session.transport.nio

import android.os.Build
import androidx.annotation.RequiresApi
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.flow
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
import java.nio.channels.AsynchronousSocketChannel
import java.util.concurrent.TimeUnit
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.coroutines.suspendCoroutine
import kotlin.math.min
import kotlin.math.roundToLong
import kotlin.time.Duration
import kotlin.time.ExperimentalTime


@ExperimentalTime
@RequiresApi(Build.VERSION_CODES.O)
class AsyncClientControlPacketTransport(
    override val scope: CoroutineScope,
    val socket: AsynchronousSocketChannel,
    override val connectionRequest: IConnectionRequest,
    override val maxBufferSize: Int
) : AbstractClientControlPacketTransport(scope, connectionRequest, maxBufferSize) {

    override fun isOpen() = try {
        socket.remoteAddress != null
    } catch (e: Exception) {
        false
    }

    private val packetBuffer =
        ByteBuffer.allocateDirect(min(maxBufferSize, socket.getOption(StandardSocketOptions.SO_RCVBUF)))


    suspend fun AsynchronousSocketChannel.suspendConnectSocket(inetSocketAddress: InetSocketAddress) {
        suspendCoroutine<Void?> { continuation ->
            try {
                socket.connect(inetSocketAddress, continuation, ConnectCompletionHandler)
            } catch (e: Exception) {
                continuation.resumeWithException(e)
            }
        }
    }

    override suspend fun open(port: UShort, host: String?): IConnectionAcknowledgment {
        socket.suspendConnectSocket(InetSocketAddress(address(host), port.toInt()))
        write(connectionRequest)
        val packet = read()
        if (packet is IConnectionAcknowledgment) {
            startWriteChannel()
            startPingTimer()
        } else {
            throw IllegalStateException("Expected a Connection Acknowledgement, got $packet instead")
        }
        return packet
    }

    override val incomingControlPackets = flow {
        while (scope.isActive) {
            val packet = read() ?: continue
            emit(packet)
        }
    }

    override suspend fun read(timeout: Duration): ControlPacket? {
        val metadata = suspendCoroutine<FixedHeaderMetadata?> { continuation ->
            scope.launch {
                socket.read(
                    packetBuffer, timeout.inMilliseconds.roundToLong(), TimeUnit.MILLISECONDS,
                    continuation, FixedHeaderCompletionHandler(packetBuffer)
                )
            }
        } ?: return null
        return if (metadata.remainingLength.toLong() < packetBuffer.remaining()) { // we already read the entire message in the buffer
            packetBuffer.read(connectionRequest.protocolVersion)
        } else {
            suspendCoroutine {
                throw UnsupportedOperationException("TODO: WIP to read buffers larger than whats larger than max buffer")
            }
        }
    }

    override suspend fun write(packet: ControlPacket, timeout: Duration): Int {
        val bytes = suspendCoroutine<Int> { continuation ->
            socket.write(
                packet.serialize().readByteBuffer(), timeout.inMilliseconds.roundToLong(),
                TimeUnit.MILLISECONDS, continuation, WriteCompletionHandler
            )
        }
        if (packet is IDisconnectNotification) {
            suspendCoroutine<Void?> { continuation ->
                try {
                    outboundChannel.close()
                    socket.close()
                } catch (e: Exception) {
                } finally {
                    continuation.resume(null)
                }
            }
        }
        return bytes
    }

    override fun assignedPort() = try {
        (socket.remoteAddress as? InetSocketAddress)?.port?.toUShort()
    } catch (e: Exception) {
        null
    }

    override fun close() {
        super.close()
        socket.close()
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
    connectionRequest: IConnectionRequest,
    scope: CoroutineScope,
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