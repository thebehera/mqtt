package mqtt.client.session.transport.nio

import android.os.Build
import androidx.annotation.RequiresApi
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.SendChannel
import kotlinx.coroutines.delay
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

@RequiresApi(Build.VERSION_CODES.O)
class AsyncClientControlPacketTransport(
    override val scope: CoroutineScope,
    val socket: AsynchronousSocketChannel,
    override val connectionRequest: IConnectionRequest,
    override val maxBufferSize: Int
) : ClientControlPacketTransport {

    override val outboundChannel: SendChannel<ControlPacket> = Channel()

    private val outbound = outboundChannel as Channel<ControlPacket>
    private val timeoutMs = connectionRequest.keepAliveTimeoutSeconds.toLong() * 1500L
    private val packetBuffer =
        ByteBuffer.allocateDirect(min(maxBufferSize, socket.getOption(StandardSocketOptions.SO_RCVBUF)))
    private var lastMessageReceived: Long = 0L

    override suspend fun open(port: Int, host: String?): IConnectionAcknowledgment {
        val address = address(host)
        suspendCoroutine<Void?> { continuation ->
            try {
                socket.connect(InetSocketAddress(address, port), continuation, ConnectCompletionHandler)
            } catch (e: Exception) {
                continuation.resumeWithException(e)
            }
        }
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
            val packet = read() ?: return@flow
            emit(packet)
        }
    }

    override fun close() {
        scope.launch { outboundChannel.send(disconnect(connectionRequest.protocolVersion)) }
    }

    private suspend inline fun readFixedHeader(timeout: Long, unit: TimeUnit): FixedHeaderMetadata? {
        // TODO: Handle if the end of stream is where the packet buffer underflows when reading the size
        val metadata = suspendCoroutine<FixedHeaderMetadata?> { continuation ->
            scope.launch {
                socket.read(
                    packetBuffer, timeout, unit, continuation,
                    FixedHeaderCompletionHandler(packetBuffer)
                )
            }
        }
        lastMessageReceived = System.currentTimeMillis()
        return metadata
    }

    private suspend inline fun readRestOfPacket(metadata: FixedHeaderMetadata): ControlPacket {
        return if (metadata.remainingLength.toLong() < packetBuffer.remaining()) { // we already read the entire message in the buffer
            packetBuffer.read(connectionRequest.protocolVersion)
        } else {
            suspendCoroutine {
                throw UnsupportedOperationException("TODO: WIP to read buffers larger than whats larger than max buffer")
            }
        }
    }

    private suspend inline fun read(timeout: Long = timeoutMs, unit: TimeUnit = TimeUnit.MILLISECONDS): ControlPacket? {
        val metadata = readFixedHeader(timeout, unit) ?: return null
        return readRestOfPacket(metadata)
    }

    private fun startPingTimer() = scope.launch {
        delay(2000)
        while (isActive && socket.isOpen) {
            outboundChannel.send(ping(connectionRequest.protocolVersion))
            delay(2000)
        }
    }

    private fun startWriteChannel() = scope.launch {
        for (packet in outbound) {
            write(packet)
        }
    }


    private suspend inline fun write(
        packet: ControlPacket,
        timeout: Long = timeoutMs,
        unit: TimeUnit = TimeUnit.MILLISECONDS
    ): Int {
        val bytes = suspendCoroutine<Int> { continuation ->
            println("writing $packet")
            socket.write(packet.serialize().readByteBuffer(), timeout, unit, continuation, WriteCompletionHandler)
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
}

suspend fun address(host: String?) = suspendCoroutine<InetAddress> {
    try {
        it.resume(InetAddress.getByName(host))
    } catch (e: Exception) {
        it.resumeWithException(e)
    }
}

@RequiresApi(Build.VERSION_CODES.O)
suspend fun asyncClientTransport(
    connectionRequest: IConnectionRequest,
    scope: CoroutineScope,
    maxBufferSize: Int = 12_000
)
        : ClientControlPacketTransport {
    val socket = suspendCoroutine<AsynchronousSocketChannel> {
        try {
            it.resume(AsynchronousSocketChannel.open())
        } catch (e: Exception) {
            it.resumeWithException(e)
        }
    }
    return AsyncClientControlPacketTransport(scope, socket, connectionRequest, maxBufferSize)
}