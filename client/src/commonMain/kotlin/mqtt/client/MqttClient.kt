package mqtt.client

import kotlinx.atomicfu.AtomicRef
import kotlinx.atomicfu.atomic
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.ClosedReceiveChannelException
import mqtt.time.currentTimestampMs
import mqtt.wire.ProtocolError
import mqtt.wire.control.packet.ControlPacket
import mqtt.wire.control.packet.IConnectionAcknowledgment
import mqtt.wire4.control.packet.DisconnectNotification
import mqtt.wire4.control.packet.PingRequest
import kotlin.coroutines.CoroutineContext

/**
 * Upon normal operation each mqtt client connection progresses through a sequence of states
 */
sealed class ConnectionState

/**
 * Initial state of each MQTT connection. Messages may be enqueued but they won't be transmitted until the
 * connection is open.
 */
object Initializing : ConnectionState()

/**
 *  Starting the connection process. Messages may be enqueued but they won't be transmitted until the
 *  connection is open.
 */
object Connecting : ConnectionState()

/**
 *  The MQTT connection has been accepted by the server and is last known to be connected since the keep alive timeout
 */
object Open : ConnectionState()

/**
 *  MQTT client has initiated a graceful shutdown. The client will attempt to dequeue all messages then send a
 *  DisconnectNotification
 */
object Closing : ConnectionState()

/**
 * The MQTT client has transmitted all of its messages and has received all messages from the peer.
 */
data class Closed(val exception: Exception? = null) : ConnectionState()

/**
 * The MQTT client connection to server failed. Messages that were successfully enqueued by either peer may not have been transmitted to the other.
 */
data class ConnectionFailure(val exception: Exception) : ConnectionState()


internal interface ISocketConnection : CoroutineScope {
    val state: AtomicRef<ConnectionState>
    val parameters: ConnectionParameters
    var connack: IConnectionAcknowledgment?
    var clientToServer: Channel<ControlPacket>
    var serverToClient: Channel<ControlPacket>
    var currentSocket: PlatformSocket?
    suspend fun buildSocket(): PlatformSocket
    fun setLastMessageReceived(time: Long)

    /**
     * Open the connection.
     * @param waitForConnectionAcknowledgment return right after the socket has been written to
     */
    fun openConnectionAsync(
            waitForConnectionAcknowledgment: Boolean = false
    ) = async {
        if (!state.compareAndSet(Initializing, Connecting)) {
            val error = ConnectionFailure(ConcurrentModificationException("Invalid previous state before connecting"))
            state.lazySet(error)
            return@async state
        }
        val platformSocketConnected = withTimeoutOrNull(parameters.connectionTimeoutMilliseconds) {
            buildSocket()
        }
        currentSocket = platformSocketConnected
        if (platformSocketConnected == null) {
            val e = ConnectionTimeout("Failed to connect within ${parameters.connectionTimeoutMilliseconds}ms")
            val connectionState = ConnectionFailure(e)
            state.lazySet(connectionState)
            return@async state
        }
        val connectionRequestJob = writeConnectionRequestAsync(platformSocketConnected)
        val connectionRequestException = connectionRequestJob.await()
        if (connectionRequestException != null) {
            return@async state
        }

        if (waitForConnectionAcknowledgment) {
            val readConnackException = readConnectionAck(platformSocketConnected)
            if (readConnackException != null) {
                return@async state
            }
            state.lazySet(Open)
            return@async state
        } else {
            if (!state.compareAndSet(Connecting, Open)) {
                throw ConcurrentModificationException("Invalid state when failing to connect launched")
            }
            launch {
                val readException = readConnectionAck(platformSocketConnected)
                if (readException != null) {
                    throw FailedToReadConnectionAck(readException.exception)
                }
            }
        }
        return@async state
    }

    fun isOpenAndActive() = isActive && state.value == Open

    fun beforeClosingSocket() {}

    private suspend fun closeSocket(e: Exception? = null): Boolean {
        if (state.value == Initializing || state.value == Connecting || state.value == Open) {
            clientToServer.send(DisconnectNotification)
            state.lazySet(Closing)
            beforeClosingSocket()
            clientToServer.close(e)
            serverToClient.close(e)
            currentSocket?.dispose()
            currentSocket = null
            return true
        }
        return false
    }

    fun closeAsync() = async {
        closeSocket()
    }

    private fun writeConnectionRequestAsync(platformSocket: PlatformSocket) = async {
        try {
            val output = platformSocket.output
            val connectionRequestPacket = parameters.connectionRequest.copy().serialize()
            val size = connectionRequestPacket.remaining
            val serializationTime = currentTimestampMs()
            output.writePacket(connectionRequestPacket)
            val postWriteTime = currentTimestampMs()
            val socketWriteTime = postWriteTime - serializationTime
            println("OUT [$size][$socketWriteTime]: ${parameters.connectionRequest}")

            return@async null
        } catch (e: Exception) {
            val failureState = ConnectionFailure(e)
            if (!state.compareAndSet(Connecting, failureState)) {
                throw ConcurrentModificationException("Invalid state when failing to connect")
            }
            return@async failureState
        }
    }

    private fun openWriteChannel(platformSocket: PlatformSocket) = launch {
        val clientToServer = Channel<ControlPacket>()
        try {
            this@ISocketConnection.clientToServer = clientToServer
            for (messageToSend in clientToServer) {
                if (isOpenAndActive() || messageToSend is DisconnectNotification) {
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
                        return@launch
                    }
                }
            }
        } finally {
            clientToServer.close()
        }
    }

    private suspend fun readConnectionAck(platformSocket: PlatformSocket): ConnectionFailure? {
        try {
            val input = platformSocket.input
            val controlPacket = input.read()
            setLastMessageReceived(currentTimestampMs())
            if (controlPacket is IConnectionAcknowledgment) {
                connack = controlPacket
                println("IN: $controlPacket")
                if (!state.compareAndSet(Connecting, Open)) {
                    throw IllegalStateException("Invalid state when reading connection ack - open")
                }
                openWriteChannel(platformSocket)
                readControlPackets(platformSocket)
                runKeepAlive()
                return null
            }
            throw ProtocolError("Invalid message received from server, expected a connection acknowledgement " +
                    "instead got: $controlPacket")
        } catch (e: Exception) {
            val failureState = ConnectionFailure(e)
            if (!state.compareAndSet(Connecting, failureState)) {
                throw ConcurrentModificationException("Invalid state reading connection ack - failure")
            }
            return failureState
        }
    }

    private fun readControlPackets(platformSocket: PlatformSocket) = launch {
        val serverToClient = Channel<ControlPacket>()
        this@ISocketConnection.serverToClient = serverToClient
        try {
            val input = platformSocket.input
            while (isOpenAndActive()) {
                val byte = input.read()
                setLastMessageReceived(currentTimestampMs())
                if (!serverToClient.offer(byte)) {
                    println("IN: $byte")
                }
            }
        } catch (e: ClosedReceiveChannelException) {
            state.lazySet(Closed(e))
        } finally {
            serverToClient.close()
        }
    }

    fun lastMessageBetweenClientAndServer(): Long

    private fun runKeepAlive() = launch {
        @Suppress("EXPERIMENTAL_API_USAGE")
        val keepAliveTimeoutSeconds = parameters.connectionRequest.keepAliveTimeoutSeconds.toLong()
        if (keepAliveTimeoutSeconds > 0) {
            val keepAliveTimeoutMs = keepAliveTimeoutSeconds * 1000
            while (isOpenAndActive()) {
                if (currentTimestampMs() - lastMessageBetweenClientAndServer() > keepAliveTimeoutMs) {
                    clientToServer.send(PingRequest)
                    delay(keepAliveTimeoutMs)
                } else {
                    delay(keepAliveTimeoutMs - (currentTimestampMs() - lastMessageBetweenClientAndServer()))
                }
            }
        }
    }
}

internal abstract class AbstractSocketConnection : ISocketConnection {
    private val job: Job = Job()
    abstract val dispatcher: CoroutineDispatcher
    override val coroutineContext: CoroutineContext get() = job + dispatcher

    override val state = atomic<ConnectionState>(Initializing)

    override var currentSocket: PlatformSocket? = null

    override var connack: IConnectionAcknowledgment? = null

    override var clientToServer: Channel<ControlPacket> = Channel()
    override var serverToClient: Channel<ControlPacket> = Channel()

    private val lastMessageBetweenClientAndServer = atomic(0L)
    override fun setLastMessageReceived(time: Long) = lastMessageBetweenClientAndServer.lazySet(time)
    override fun lastMessageBetweenClientAndServer(): Long = lastMessageBetweenClientAndServer.value
}

internal expect class PlatformSocketConnection : AbstractSocketConnection

interface MessageQueue

interface Engine {
    //    val connection: SocketConnection
    val messageQueue: MessageQueue
}

interface Client {
    val engine: Engine

}
