@file:Suppress("EXPERIMENTAL_API_USAGE")

package mqtt.client

import kotlinx.atomicfu.atomic
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.SendChannel
import kotlinx.coroutines.io.ByteReadChannel
import kotlinx.coroutines.io.ByteWriteChannel
import kotlinx.coroutines.io.writeFully
import kotlinx.io.core.readBytes
import kotlinx.io.errors.IOException
import mqtt.time.currentTimestampMs
import mqtt.wire.control.packet.ControlPacket
import mqtt.wire.control.packet.IConnectionRequest
import mqtt.wire4.control.packet.ConnectionAcknowledgment
import mqtt.wire4.control.packet.DisconnectNotification
import mqtt.wire4.control.packet.PingRequest
import kotlin.coroutines.CoroutineContext

data class ConnectionParameters(val hostname: String,
                                val port: Int,
                                val connectionRequest: IConnectionRequest,
                                val reconnectIfNetworkLost: Boolean = true,
                                val clientToBroker: Channel<ControlPacket> = Channel(),
                                val brokerToClient: SendChannel<ControlPacket> = Channel())

abstract class AbstractConnection : IConnection {
    override var job: Job = Job()
    private val _lastMessageBetweenClientAndServer = atomic(0L)
    override fun lastMessageBetweenClientAndServer() = _lastMessageBetweenClientAndServer.value
    protected val _isConnectedOrConnecting = atomic(false)
    override fun isConnectedOrConnecting() = _isConnectedOrConnecting.value
    val connectionAttemptTime = atomic(0L)

    override fun transitionIntoConnecting(): Boolean {
        if (!_isConnectedOrConnecting.compareAndSet(false, true)) {
            println("Already connected or connecting -- open")
            return false
        }
        return true
    }

    override fun transitionIntoDisconnected(): Boolean {
        if (!_isConnectedOrConnecting.compareAndSet(true, false)) {
            println("failed to set the disconnected state")
            return false
        }
        return true
    }

    override suspend fun writeConnectionRequest() {
        val output = platformSocket.output
        val writeChannelTime = currentTimestampMs()
        val connectionRequestPacket = parameters.connectionRequest.copy().serialize().readBytes()
        val serializationTime = currentTimestampMs()
        val processTime = serializationTime - writeChannelTime
        println("Socket processing time took $processTime ms")
        output.writeFully(connectionRequestPacket)
        val postWriteTime = currentTimestampMs()
        val socketWriteTime = postWriteTime - serializationTime
        println("OUT [${connectionRequestPacket.size}][$socketWriteTime]: ${parameters.connectionRequest}")
    }

    override fun setLastMessageReceived(timestamp: Long) = _lastMessageBetweenClientAndServer.lazySet(timestamp)

}

interface IConnection : CoroutineScope {
    val parameters: ConnectionParameters
    val job: Job
    val dispatcher: CoroutineDispatcher
    var platformSocket: PlatformSocket
    override val coroutineContext: CoroutineContext get() = dispatcher + job
    fun lastMessageBetweenClientAndServer(): Long
    fun isConnectedOrConnecting(): Boolean
    fun startAsync() = async {
        if (!beforeOpeningSocket()) {
            return@async false
        }
        return@async openSocket()
    }

    suspend fun writeConnectionRequest()
    fun beforeOpeningSocket(): Boolean {
        if (isConnectedOrConnecting()) {
            println("Already connected or connecting -- start")
            return false
        }
        return true
    }

    suspend fun openSocket(): Boolean = coroutineScope {
        if (!transitionIntoConnecting()) {
            return@coroutineScope false
        }
        platformSocket = buildSocket()
        writeConnectionRequest()
        return@coroutineScope try {
            startReadingFromSocket()
            startWritingToSocket()
            suspendUntilSocketClose()
            println("socket closed")
            true
        } catch (e: CancellationException) {
            println(e)
            false
        } finally {
            closeSocket()
        }
    }

    fun CoroutineScope.startReadingFromSocket() = launch {
        val input = platformSocket.input
        println("open reading channel")
        val controlPacket = input.read()
        println("read first byte")
        if (controlPacket is ConnectionAcknowledgment) {
            println("IN: $controlPacket")
            runKeepAlive()
        }
        while (isActive) {
            val byte = input.read()
            println("IN: $byte")
            setLastMessageReceived(currentTimestampMs())
            GlobalScope.launch(Dispatchers.Unconfined) { parameters.brokerToClient.send(controlPacket) }
        }
    }

    fun transitionIntoConnecting(): Boolean
    fun transitionIntoDisconnected(): Boolean

    suspend fun buildSocket(): PlatformSocket

    fun CoroutineScope.runKeepAlive() = launch {
        val keepAliveTimeoutSeconds = parameters.connectionRequest.keepAliveTimeoutSeconds.toLong()
        if (keepAliveTimeoutSeconds > 0) {
            val keepAliveTimeoutMs = keepAliveTimeoutSeconds * 1000
            while (isActive) {
                if (currentTimestampMs() - lastMessageBetweenClientAndServer() > keepAliveTimeoutMs) {
                    parameters.clientToBroker.send(PingRequest)
                    delay(keepAliveTimeoutMs)
                } else {
                    delay(keepAliveTimeoutMs - (currentTimestampMs() - lastMessageBetweenClientAndServer()))
                }
            }
        }
    }

    fun setLastMessageReceived(timestamp: Long)

    suspend fun startWritingToSocket() {
        for (messageToSend in parameters.clientToBroker) {
            if (isActive || messageToSend is DisconnectNotification) {
                val sendMessage = messageToSend.serialize()
                val size = sendMessage.remaining
                val start = currentTimestampMs()
                platformSocket.output.writePacket(sendMessage)
                val writeComplete = currentTimestampMs()
                setLastMessageReceived(writeComplete)
                val sendTime = writeComplete - start
                println("OUT [$size][$sendTime]: $messageToSend")
                if (messageToSend is DisconnectNotification) {
                    platformSocket.dispose()
                    println("socket disposed")
                }
            }
        }
    }

    fun send(packet: ControlPacket) = launch {
        parameters.clientToBroker.send(packet)
    }

    suspend fun suspendUntilSocketClose() {
        platformSocket.awaitClosed()
    }

    fun beforeClosingSocket()

    fun closeSocket() {
        println("socket is closed: ${platformSocket.isClosed}")
        if (transitionIntoDisconnected()) {
            throw ConcurrentModificationException("Invalid connectivity state")
        }
    }
}


expect class Connection(parameters: ConnectionParameters) : AbstractConnection

fun openConnection(parameters: ConnectionParameters) = GlobalScope.async {
    if (parameters.reconnectIfNetworkLost) {
        var oldConnection: Connection
        retryIO {
            oldConnection = Connection(parameters)
            val connection = oldConnection.startAsync()
            connection.await()
        }
        return@async false
    } else {
        val connection = Connection(parameters)
        val result = connection.startAsync()
        result.await()
        return@async result.getCompleted()
    }
}

suspend fun retryIO(
        times: Int = Int.MAX_VALUE,
        initialDelay: Long = 100, // 0.1 second
        maxDelay: Long = 1000,    // 1 second
        factor: Double = 2.0,
        block: suspend () -> Unit) {
    var currentDelay = initialDelay
    repeat(times - 1) {
        try {
            block()
        } catch (e: IOException) {
            println("IOException retrying in $currentDelay ms")
        } catch (e: Exception) {
            // you can log an error here and/or make a more finer-grained
            // analysis of the cause to see if retry is needed
            println("error while retrying: $e")
        }
        delay(currentDelay)
        currentDelay = (currentDelay * factor).toLong().coerceAtMost(maxDelay)
    }
    return block() // last attempt
}

interface PlatformSocket {
    val output: ByteWriteChannel
    val input: ByteReadChannel
    fun dispose()
    suspend fun awaitClosed()
    val isClosed: Boolean
}
