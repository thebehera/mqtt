package mqtt.transport

import kotlinx.coroutines.*
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.SendChannel
import kotlinx.coroutines.channels.consumeEach
import kotlinx.coroutines.flow.consumeAsFlow
import kotlinx.coroutines.sync.Mutex
import mqtt.connection.ControlPacketTransport
import mqtt.time.currentTimestampMs
import mqtt.wire.control.packet.ControlPacket
import mqtt.wire.control.packet.IConnectionRequest
import mqtt.wire.control.packet.IDisconnectNotification
import mqtt.wire4.control.packet.DisconnectNotification
import kotlin.time.Duration
import kotlin.time.ExperimentalTime
import kotlin.time.measureTime


@ExperimentalTime
abstract class AbstractClientControlPacketTransport(
    override val scope: CoroutineScope,
    val connectionRequest: IConnectionRequest,
    val timeout: Duration,
    val timeoutMultiplier: Double = 1.5,
    override val maxBufferSize: Int
) : ControlPacketTransport {
    internal val protocolVersion = connectionRequest.protocolVersion
    override val outboundChannel: SendChannel<ControlPacket> = Channel()
    protected val outbound by lazy { this.outboundChannel as Channel<ControlPacket> }
    final override val inboxChannel = Channel<ControlPacket>(Channel.UNLIMITED)
    protected var lastMessageReadAt: Long = currentTimestampMs()
    override var completedWrite: SendChannel<ControlPacket>? = null
    protected var isClosing = false

    protected fun startWriteChannel() = scope.launch {
        try {
            outbound.consumeEach { packet ->
                write(packet, timeout)
                try {
                    completedWrite?.send(packet)
                } catch (e: CancellationException) {
                    completedWrite?.close()
                }
            }
        } catch (e: Throwable) {
//            println("closed with exception $e")
        } finally {
            suspendClose()
        }
    }


    protected abstract suspend fun read(timeout: Duration): ControlPacket
    protected abstract suspend fun write(packet: ControlPacket, timeout: Duration): Int

    protected fun startReadChannel() = scope.launch {
        var startTime = currentTimestampMs()
        try {
            while (scope.isActive) {
                startTime = currentTimestampMs()
                val packetRead = read(timeout * timeoutMultiplier)
                lastMessageReadAt = currentTimestampMs()
                inboxChannel.send(packetRead)
            }
        } catch (e: Throwable) {
//            println("read channel closed with exception $e")
        } finally {
            println("closing after ${currentTimestampMs() - startTime}ms")
            suspendClose()
        }
    }

    protected fun nextDelay(keepAliveMs: Long): Long {
        val nextMessageTime = lastMessageReadAt + keepAliveMs
        val time = currentTimestampMs()
        var deltaTime = nextMessageTime - time
        if (deltaTime < 0) {
            deltaTime = keepAliveMs
        }
        return deltaTime
    }

    protected suspend fun delayUntilPingInterval(keepAliveMs: Long) {
        val deltaTime = nextDelay(keepAliveMs)
        delay(deltaTime)
    }

    override val incomingControlPackets = inboxChannel.consumeAsFlow()

    override suspend fun suspendClose() {
        try {
            if (outbound.isClosedForSend) {
                return
            }
            try {
                isClosing = true
                outbound.send(disconnect(protocolVersion))
                val time = measureTime {
                    val mutex = Mutex(true)
                    try {
                        outbound.invokeOnClose {
                            mutex.unlock()
                        }
                        mutex.lock()
                    } catch (e: IllegalStateException) {
                        println("ignoring $e")
                    }

                }
                println("sent suspend close and suspended for $time")
            } catch (e: CancellationException) {
                println("suspend close cancelled with Exception $e")
            }
        } finally {
            close()
        }
    }

    override fun close() {
        inboxChannel.close()
        outboundChannel.close()
        completedWrite?.close()
    }
}

fun disconnect(protocolVersion: Int): IDisconnectNotification {
    return when (protocolVersion) {
        3, 4 -> DisconnectNotification
        5 -> mqtt.wire5.control.packet.DisconnectNotification()
        else -> throw IllegalArgumentException("Received an unsupported protocol version $protocolVersion")
    }
}