package mqtt.client

import kotlinx.atomicfu.atomic
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.ClosedReceiveChannelException
import kotlinx.coroutines.io.ClosedWriteChannelException
import mqtt.time.currentTimestampMs
import mqtt.wire.ProtocolError
import mqtt.wire.control.packet.ControlPacket
import mqtt.wire.control.packet.IConnectionAcknowledgment
import mqtt.wire4.control.packet.DisconnectNotification
import mqtt.wire4.control.packet.PingRequest
import kotlin.coroutines.CoroutineContext

abstract class SocketSession : CoroutineScope {
    private val job: Job = Job()
    abstract val dispatcher: CoroutineDispatcher
    override val coroutineContext: CoroutineContext get() = job + dispatcher
    abstract val parameters: ConnectionParameters
    val state = atomic<ConnectionState>(Initializing)

    var currentSocket: PlatformSocket? = null

    var connack: IConnectionAcknowledgment? = null

    var clientToServer: Channel<ControlPacket> = Channel()
    var serverToClient: Channel<ControlPacket> = Channel()

    private val lastMessageBetweenClientAndServer = atomic(0L)
    fun setLastMessageReceived(time: Long) = lastMessageBetweenClientAndServer.lazySet(time)

    abstract suspend fun buildSocket(): PlatformSocket

    /**
     * Open the connection.
     * @param waitForConnectionAcknowledgment return right after the socket has been written to
     */
    fun openConnectionAsync(waitForConnectionAcknowledgment: Boolean = false) = async {
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
            println("open")
            return@async state
        } else {
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

    open fun beforeClosingSocket() {}


    fun closeAsync() = async {
        if (state.value == Initializing || state.value == Connecting || state.value == Open) {
            clientToServer.send(DisconnectNotification)
            while (isActive && state.value !is Closed) {
            }
            return@async true
        }
        return@async false
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
        try {
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
                        hardClose()
                        return@launch
                    }
                }
            }
        } catch (e: ClosedWriteChannelException) {
            hardClose(e)
        } finally {
            if (state.value !is Closed) {
                hardClose()
            }
        }
    }

    suspend fun hardClose(e: Exception? = null) {
        if (state.value is Closed) {
            return
        }
        state.lazySet(Closing)
        beforeClosingSocket()
        clientToServer.close()
        serverToClient.close()
        currentSocket?.dispose()
        state.lazySet(Closed(e))
        currentSocket = null
        currentSocket?.awaitClosed()
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
                    throw IllegalStateException("Invalid state when reading connection ack - open (is ${state.value})")
                }
                openWriteChannel(platformSocket)
                readControlPackets(platformSocket)
                runKeepAlive()
                return null
            }
            throw ProtocolError("Invalid message received from server, expected a connection acknowledgement " +
                    "instead got: $controlPacket")
        } catch (e: Exception) {
            return ConnectionFailure(e)
        }
    }

    private fun readControlPackets(platformSocket: PlatformSocket) = launch {
        try {
            val input = platformSocket.input
            while (isOpenAndActive()) {
                val controlPacket = input.read()
                println("INNNN $serverToClient: $controlPacket")
                setLastMessageReceived(currentTimestampMs())
                if (!serverToClient.offer(controlPacket)) {
                    println("NO PICKUP IN: $controlPacket")
                }
            }
        } catch (e: ClosedReceiveChannelException) {
            hardClose(e)
            state.lazySet(Closed(e))
        } finally {
            if (state.value !is Closed) {
                hardClose()
            }
        }
    }

    fun lastMessageBetweenClientAndServer(): Long = lastMessageBetweenClientAndServer.value

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
