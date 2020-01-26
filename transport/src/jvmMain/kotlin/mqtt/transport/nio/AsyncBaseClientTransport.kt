package mqtt.transport.nio

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.channels.ClosedSendChannelException
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import mqtt.transport.AbstractClientControlPacketTransport
import mqtt.transport.nio.util.*
import mqtt.wire.control.packet.ControlPacket
import mqtt.wire.control.packet.IConnectionRequest
import mqtt.wire.control.packet.IDisconnectNotification
import java.net.StandardSocketOptions
import java.nio.ByteBuffer
import java.nio.channels.AsynchronousSocketChannel
import kotlin.math.min
import kotlin.time.Duration
import kotlin.time.ExperimentalTime

@ExperimentalTime
open class AsyncBaseClientTransport(
    override val scope: CoroutineScope,
    open val socket: AsynchronousSocketChannel,
    connectionRequest: IConnectionRequest,
    override val maxBufferSize: Int,
    timeout: Duration, timeoutMultiplier: Double = 1.5
) : AbstractClientControlPacketTransport(scope, connectionRequest, timeout, timeoutMultiplier, maxBufferSize) {
    private val packetBuffer: ByteBuffer by lazy {
        ByteBuffer.allocateDirect(
            min(
                maxBufferSize,
                socket.getOption(StandardSocketOptions.SO_RCVBUF)
            )
        )
    }

    protected fun startPingTimer() = scope.launch {
        while (isActive) {
            delayUntilPingInterval(connectionRequest.keepAliveTimeoutSeconds.toLong() * 1000L)
            try {
                outboundChannel.send(ping(connectionRequest.protocolVersion))
            } catch (e: ClosedSendChannelException) {
                return@launch
            }
        }
    }

    override suspend fun read(timeout: Duration) = socket.aReadPacket(packetBuffer, scope, protocolVersion, timeout)

    override suspend fun write(packet: ControlPacket, timeout: Duration): Int {
        val bytes = socket.writePacket(packet, timeout)
        if (packet !is IDisconnectNotification) {
            return bytes
        }
        socket.aClose()
        outbound.close()
        inboxChannel.close()
        return bytes
    }

    override fun assignedPort() = socket.assignedPort()

    override fun isOpen() = socket.isOpen
}