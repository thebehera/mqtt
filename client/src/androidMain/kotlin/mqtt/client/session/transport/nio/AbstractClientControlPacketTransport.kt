package mqtt.client.session.transport.nio

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.SendChannel
import kotlinx.coroutines.channels.sendBlocking
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.consumeAsFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import mqtt.connection.ClientControlPacketTransport
import mqtt.time.currentTimestampMs
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
    protected val inboxChannel = Channel<ControlPacket>(Channel.UNLIMITED)
    private var lastMessageReadAt: Long = currentTimestampMs()
    override var completedWrite: SendChannel<ControlPacket>? = null
    abstract fun isOpen(): Boolean
    private var isClosing = false

    protected fun startPingTimer() = scope.launch {
        delayUntilPingInterval()
        while (!isClosing && isActive && isOpen()) {
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

    protected fun startWriteChannel() = scope.launch {
        try {
            for (packet in outbound) {
                write(packet)
                val outboundCompletion = completedWrite ?: continue
                scope.launch {
                    outboundCompletion.send(packet)
                }
            }
            outbound.close()
        } catch (e: Exception) {
            outbound.close(e)
        }
    }

    protected fun startReadChannel() = scope.launch {
        try {
            while (!isClosing && scope.isActive && isOpen()) {
                inboxChannel.send(read())
            }
            inboxChannel.close()
        } catch (e: Exception) {
            inboxChannel.close(e)
        }
    }

    override val incomingControlPackets = inboxChannel.consumeAsFlow()

    protected abstract suspend fun read(timeout: Duration = timeoutMs.milliseconds): ControlPacket
    protected abstract suspend fun write(packet: ControlPacket, timeout: Duration = timeoutMs.milliseconds): Int

    override fun close() {
        isClosing = true
        outboundChannel.sendBlocking(disconnect(connectionRequest.protocolVersion))
        outboundChannel.close()
        completedWrite?.close()
    }
}