package mqtt.client.session.transport.nio

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.SendChannel
import kotlinx.coroutines.flow.consumeAsFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import mqtt.connection.ControlPacketTransport
import mqtt.time.currentTimestampMs
import mqtt.wire.control.packet.ControlPacket
import kotlin.time.Duration
import kotlin.time.ExperimentalTime

@ExperimentalTime
abstract class AbstractClientControlPacketTransport(
    override val scope: CoroutineScope,
    val protocolVersion: Int,
    val timeout: Duration,
    override val maxBufferSize: Int
) : ControlPacketTransport {

    override val outboundChannel: SendChannel<ControlPacket> = Channel()
    protected val outbound by lazy { this.outboundChannel as Channel<ControlPacket> }
    final override val inboxChannel = Channel<ControlPacket>(Channel.UNLIMITED)
    protected var lastMessageReadAt: Long = currentTimestampMs()
    override var completedWrite: SendChannel<ControlPacket>? = null
    protected var isClosing = false

    protected fun startWriteChannel() = scope.launch {
        try {
            for (packet in outbound) {
                write(packet, timeout)
                val outboundCompletion = completedWrite ?: continue
                scope.launch {
                    outboundCompletion.send(packet)
                }
            }
            completedWrite?.close()
            outbound.close()
        } catch (e: Exception) {
            completedWrite?.close(e)
            outbound.close(e)
        }
    }

    protected fun startReadChannel() = scope.launch {
        try {
            while (scope.isActive) {
                inboxChannel.send(read(timeout * 1.5))
            }
            inboxChannel.close()
        } catch (e: Exception) {
            inboxChannel.close(e)
        }
    }

    override val incomingControlPackets = inboxChannel.consumeAsFlow()

    override fun close() {
        inboxChannel.close()
        println("inbox channel close")
        outboundChannel.close()
        println("outbound channel close")
        completedWrite?.close()
        println("completed write channel close")
    }
}