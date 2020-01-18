package mqtt.client.session.transport.nio

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.SendChannel
import kotlinx.coroutines.channels.consumeEach
import kotlinx.coroutines.flow.consumeAsFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import mqtt.connection.ControlPacketTransport
import mqtt.time.currentTimestampMs
import mqtt.wire.control.packet.ControlPacket
import kotlin.time.Duration
import kotlin.time.ExperimentalTime
import kotlin.time.measureTime

@ExperimentalTime
abstract class AbstractClientControlPacketTransport(
    override val scope: CoroutineScope,
    val protocolVersion: Int,
    val timeout: Duration,
    val timeoutMultiplier: Double = 1.5,
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
            outbound.consumeEach { packet ->
                println("consume start")
                write(packet, timeout)
                try {
                    completedWrite?.send(packet)
                } catch (e: Throwable) {
                    println("got exception while trying to send write packtet $e")
                    completedWrite?.close(e)
                }
                println("consume end")
            }
        } catch (e: CancellationException) {
            println("cancellation e $e")
            // ignore cancellation exceptions
        } catch (e: Throwable) {
            println("closed with exception $e")
        } finally {
            suspendClose()
            close()
        }
    }

    protected fun startReadChannel() = scope.launch {
        try {
            while (scope.isActive) {
                inboxChannel.send(read(timeout * timeoutMultiplier))
            }
            println("read channel close normallly")
        } catch (e: Throwable) {
            println("read channel closed with exception $e")
        } finally {
            suspendClose()
            close()
        }
    }

    override val incomingControlPackets = inboxChannel.consumeAsFlow()

    override suspend fun suspendClose() {
        if (outbound.isClosedForSend) {
            return
        }
        try {
            isClosing = true
            println("sending suspend close")
            outbound.send(disconnect(protocolVersion))
            println("waiting for mutex")
            val time = measureTime {
                val mutex = Mutex(true)
                outbound.invokeOnClose {
                    mutex.unlock()
                    println("unlock")
                }
                mutex.lock()
            }
            println("sent suspend close and suspended for $time")
        } catch (e: Exception) {
            println("suspend close cancelled with Exception $e")
        }
    }

    override fun close() {
        inboxChannel.close()
        outboundChannel.close()
        completedWrite?.close()
    }
}