package mqtt.client.session.transport.nio

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.SendChannel
import kotlinx.coroutines.channels.sendBlocking
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import mqtt.connection.ClientControlPacketTransport
import mqtt.wire.control.packet.ControlPacket
import mqtt.wire.control.packet.IConnectionRequest
import kotlin.time.Duration
import kotlin.time.ExperimentalTime
import kotlin.time.milliseconds

@ExperimentalTime
abstract class AbstractClientControlPacketTransport(
    override val scope: CoroutineScope,
    override val connectionRequest: IConnectionRequest,
    override val maxBufferSize: Int,
    private val timeoutMs: Long = connectionRequest.keepAliveTimeoutSeconds.toLong() * 1500L
) :
    ClientControlPacketTransport {
    override val outboundChannel: SendChannel<ControlPacket> = Channel()
    protected val outbound by lazy { this.outboundChannel as Channel<ControlPacket> }

    override var completedWrite: SendChannel<ControlPacket>? = null
    abstract fun isOpen(): Boolean

    protected fun startPingTimer() = scope.launch {
        delay(connectionRequest.keepAliveTimeoutSeconds.toLong() * 1000L)
        while (isActive && isOpen()) {
            outboundChannel.send(ping(connectionRequest.protocolVersion))
            delay(connectionRequest.keepAliveTimeoutSeconds.toLong() * 1000L)
        }
    }

    protected fun startWriteChannel() = scope.launch {
        for (packet in outbound) {
            write(packet)
            val outboundCompletion = completedWrite ?: continue
            scope.launch {
                outboundCompletion.send(packet)
            }
        }
        outbound.close()
    }

    override val incomingControlPackets = flow {
        while (scope.isActive && isOpen()) {
            val packet = read() ?: return@flow
            emit(packet)
        }
    }

    protected abstract suspend fun read(timeout: Duration = timeoutMs.milliseconds): ControlPacket?
    protected abstract suspend fun write(packet: ControlPacket, timeout: Duration = timeoutMs.milliseconds): Int

    override fun close() {
        outboundChannel.sendBlocking(disconnect(connectionRequest.protocolVersion))
        outbound.close()
        completedWrite?.close()
    }
}