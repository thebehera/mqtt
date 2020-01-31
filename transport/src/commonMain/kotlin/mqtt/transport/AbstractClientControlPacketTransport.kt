package mqtt.transport

import kotlinx.coroutines.*
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.ClosedSendChannelException
import kotlinx.coroutines.channels.SendChannel
import kotlinx.coroutines.channels.consumeEach
import kotlinx.coroutines.flow.consumeAsFlow
import kotlinx.coroutines.sync.Mutex
import mqtt.connection.ClientControlPacketTransport
import mqtt.time.currentTimestampMs
import mqtt.wire.control.packet.*
import mqtt.wire4.control.packet.DisconnectNotification
import mqtt.wire4.control.packet.PingRequest
import mqtt.wire4.control.packet.PingResponse
import kotlin.time.Duration
import kotlin.time.ExperimentalTime


@ExperimentalTime
abstract class AbstractClientControlPacketTransport(
    override val scope: CoroutineScope,
    connectionRequest: IConnectionRequest,
    val timeout: Duration,
    val timeoutMultiplier: Double = 1.5,
    override val maxBufferSize: Int
) : ClientControlPacketTransport {
    internal val protocolVersion = connectionRequest.protocolVersion
    override val outboundChannel: SendChannel<ControlPacket> = Channel()
    protected val outbound by lazy { this.outboundChannel as Channel<ControlPacket> }
    final override val inboxChannel = Channel<ControlPacket>(Channel.UNLIMITED)
    protected var lastMessageReadAt: Long = currentTimestampMs()
    override var completedWrite: Channel<ControlPacket>? = null
    protected var isClosing = false

    protected fun startWriteChannel() = scope.launch {
        try {
            println("start write channel")
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


    protected fun startPingTimer() = scope.launch {
        println("start ping timer")
        val delay = connectionRequest.keepAliveTimeoutSeconds.toLong() * 1000L
        while (isActive && !isClosing) {
            delayUntilPingInterval(delay)
            try {
                outboundChannel.send(ping(connectionRequest.protocolVersion))
            } catch (e: ClosedSendChannelException) {
                return@launch
            }
        }
    }

    private fun ping(protocolVersion: Int): IPingRequest {
        return when (protocolVersion) {
            3, 4 -> PingRequest
            5 -> mqtt.wire5.control.packet.PingRequest
            else -> throw IllegalArgumentException("Received an unsupported protocol version $protocolVersion")
        }
    }


    private fun pong(protocolVersion: Int): IPingResponse {
        return when (protocolVersion) {
            3, 4 -> PingResponse
            5 -> mqtt.wire5.control.packet.PingResponse
            else -> throw IllegalArgumentException("Received an unsupported protocol version $protocolVersion")
        }
    }

    protected abstract suspend fun read(timeout: Duration): ControlPacket
    protected abstract suspend fun write(packet: ControlPacket, timeout: Duration): Int

    protected fun startReadChannel() = scope.launch {
        try {
            println("start read channel")
            while (scope.isActive && !isClosing) {
                val readTimeout = timeout
//                println("$this@AbstractClientControlPacketTransport reading channel with delay $readTimeout")
                val packetRead = read(readTimeout)
                if (packetRead is IPingRequest) {
                    outboundChannel.send(pong(connectionRequest.protocolVersion))
                }
                lastMessageReadAt = currentTimestampMs()
                inboxChannel.send(packetRead)
            }
        } catch (e: Throwable) {
            println("$this@AbstractClientControlPacketTransport read channel closed with exception $e")
        } finally {
            println("done reading, close")
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
        do {
            var delay = nextDelay(keepAliveMs)
            println("${currentTimestampMs()} delay by $delay $keepAliveMs")
            delay(delay)
            delay = nextDelay(keepAliveMs)
        } while (delay > 0 && !isClosing && scope.isActive)
    }

    override val incomingControlPackets = inboxChannel.consumeAsFlow()

    override suspend fun suspendClose() {
        println("suspend close")
        isClosing = true
        try {
            if (outbound.isClosedForSend) {
                return
            }
            try {
                println("$this sending disconnect")
                outbound.send(disconnect(protocolVersion))
                val mutex = Mutex(true)
                try {
                    outbound.invokeOnClose {
                        mutex.unlock()
                    }
                    mutex.lock()
                } catch (e: IllegalStateException) {
                    println("ignoring $e")
                }
            } catch (e: CancellationException) {
                println("suspend close cancelled with Exception $e")
            }
        } finally {
            close()
        }
    }

    override fun close() {
        println("close")
        isClosing = true
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