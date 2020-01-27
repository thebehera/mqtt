package mqtt.transport.nio2

import kotlinx.coroutines.CoroutineScope
import mqtt.transport.AbstractClientControlPacketTransport
import mqtt.transport.nio2.util.aClose
import mqtt.transport.nio2.util.aReadPacket
import mqtt.transport.nio2.util.assignedPort
import mqtt.transport.nio2.util.writePacket
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
abstract class AsyncBaseClientTransport(
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