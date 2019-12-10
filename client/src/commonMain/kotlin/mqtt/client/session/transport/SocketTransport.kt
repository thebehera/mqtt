package mqtt.client.session.transport

import io.ktor.client.HttpClient
import io.ktor.client.features.websocket.WebSockets
import io.ktor.client.features.websocket.webSocketSession
import io.ktor.client.request.request
import io.ktor.http.URLProtocol.Companion.WS
import io.ktor.http.URLProtocol.Companion.WSS
import io.ktor.util.KtorExperimentalAPI
import kotlinx.atomicfu.atomic
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.ClosedReceiveChannelException
import kotlinx.coroutines.io.ClosedWriteChannelException
import mqtt.ConnectionTimeout
import mqtt.FailedToReadConnectionAck
import mqtt.connection.*
import mqtt.time.currentTimestampMs
import mqtt.wire.ProtocolError
import mqtt.wire.control.packet.ControlPacket
import mqtt.wire.control.packet.IConnectionAcknowledgment
import mqtt.wire4.control.packet.DisconnectNotification
import mqtt.wire4.control.packet.PingRequest
import platform.Platform
import kotlin.coroutines.CoroutineContext

@KtorExperimentalAPI
abstract class SocketTransport(override val coroutineContext: CoroutineContext) : CoroutineScope {
    abstract val remoteHost: IRemoteHost

    val state = atomic<ConnectionState>(Initializing)

    var currentSocket: Transport? = null

    var outboundCallback: ((ControlPacket, Int) -> Unit)? = null
    var connack: IConnectionAcknowledgment? = null

    val clientToServer: Channel<ControlPacket> = Channel()
    var messageReceiveCallback: OnMessageReceivedCallback? = null
    open val supportsNativeSockets: Boolean = false
    private val lastMessageBetweenClientAndServer = atomic(0L)
    fun setLastMessageReceived(time: Long) = lastMessageBetweenClientAndServer.lazySet(time)

    val httpClient by lazy {
        HttpClient {
            install(WebSockets)
        }
    }

    open suspend fun buildNativeSocket(): Transport =
        throw UnsupportedOperationException("Native sockets are not supported on ${Platform.name} yet")

    suspend fun buildSocket(): Transport {
        if (remoteHost.websocket.isEnabled || !supportsNativeSockets) {
            if (!supportsNativeSockets) {
                println("W: Platform does not currently support native sockets, defaulting to websockets")
            }
            val session =
                httpClient.webSocketSession(host = remoteHost.name, port = remoteHost.port.toInt(), path = "/mqtt") {
                request {
                    url.protocol = if (remoteHost.security.isTransportLayerSecurityEnabled) {
                        WSS
                    } else {
                        WS
                    }
                }
                headers["Sec-WebSocket-Protocol"] = "mqttv3.1"
            }
            session.flush()
            return WebSocketTransport(remoteHost.request.protocolVersion, session, coroutineContext)
        } else {
            return buildNativeSocket()
        }
    }

    /**
     * Open the transport.
     * @param waitForConnectionAcknowledgment return right after the socket has been written to
     */
    fun openConnectionAsync(waitForConnectionAcknowledgment: Boolean = false) = async {
        if (!state.compareAndSet(Initializing, Connecting)) {
            val error = ConnectionFailure(ConcurrentModificationException("Invalid previous state before connecting"))
            state.lazySet(error)
            return@async state
        }
        var socketConnectException: Throwable? = null
        val platformSocketConnected = withTimeoutOrNull(remoteHost.connectionTimeout) {
            try {
                buildSocket()
            } catch (e: Exception) {
                socketConnectException = e
                return@withTimeoutOrNull null
            }
        }
        currentSocket = platformSocketConnected
        if (platformSocketConnected == null) {
            val e = socketConnectException
                ?: ConnectionTimeout("Failed to connect within ${remoteHost.connectionTimeout}ms")
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

    fun isOpenAndActive() = isActive && state.value is Open

    open fun beforeClosingSocket() {}

    suspend fun awaitSocketClose() = currentSocket?.awaitClosed()

    fun closeAsync() = async {
        if (state.value == Initializing || state.value == Connecting || state.value is Open) {
            val packet: ControlPacket = if (remoteHost.request.protocolVersion == 5) {
                mqtt.wire5.control.packet.DisconnectNotification()
            } else {
                DisconnectNotification
            }
            clientToServer.send(packet)
            while (isActive && state.value !is Closed) {
            }
            return@async true
        }
        return@async false
    }

    private fun writeConnectionRequestAsync(transport: Transport) = async {
        try {
            val host = remoteHost
            val connectionRequestPacket = host.request.copy().serialize()
            transport.writePacket(connectionRequestPacket)
            return@async null
        } catch (e: Exception) {
            val failureState = ConnectionFailure(e)
            if (!state.compareAndSet(Connecting, failureState)) {
                throw ConcurrentModificationException("Invalid state when failing to connect")
            }
            return@async failureState
        }
    }

    private fun openWriteChannel(transport: Transport) = launch {
        try {
            for (messageToSend in clientToServer) {
                if (isOpenAndActive() || messageToSend is DisconnectNotification) {
                    val sendMessage = messageToSend.serialize()
                    transport.writePacket(sendMessage)
                    val writeComplete = currentTimestampMs()
                    setLastMessageReceived(writeComplete)
                    outboundCallback?.invoke(messageToSend, remoteHost.connectionIdentifier())
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
        currentSocket?.dispose()
        state.lazySet(Closed(e))
        currentSocket = null
        currentSocket?.awaitClosed()
    }

    private suspend fun readConnectionAck(transport: Transport): ConnectionFailure? {
        try {
            val controlPacket = transport.read()
            setLastMessageReceived(currentTimestampMs())
            if (controlPacket is IConnectionAcknowledgment) {
                connack = controlPacket
                messageReceiveCallback?.onMessage(controlPacket)

                check(
                    state.compareAndSet(
                        Connecting,
                        Open(controlPacket)
                    )
                ) { "Invalid state when reading transport ack - open (is ${state.value})" }
                openWriteChannel(transport)
                readControlPackets(transport)
                runKeepAlive()
                return null
            }
            throw ProtocolError("Invalid message received from server, expected a transport acknowledgement " +
                    "instead got: $controlPacket")
        } catch (e: Exception) {
            return ConnectionFailure(e)
        }
    }

    private fun readControlPackets(transport: Transport) = launch {
        try {
            while (isOpenAndActive()) {
                val msg = transport.read()
                readControlPacket(msg)
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

    private fun readControlPacket(controlPacket: ControlPacket) {
        val newMessageReceived = currentTimestampMs()
        setLastMessageReceived(newMessageReceived)
        messageReceiveCallback?.onMessage(controlPacket)
    }

    fun lastMessageBetweenClientAndServer(): Long = lastMessageBetweenClientAndServer.value

    private fun runKeepAlive() = launch {
        @Suppress("EXPERIMENTAL_API_USAGE")
        val keepAliveTimeoutSeconds = remoteHost.request.keepAliveTimeoutSeconds.toLong()
        if (keepAliveTimeoutSeconds > 0) {
            val keepAliveTimeoutMs = keepAliveTimeoutSeconds * 1000
            while (isOpenAndActive()) {
                if (currentTimestampMs() - lastMessageBetweenClientAndServer() > keepAliveTimeoutMs) {
                    clientToServer.send(
                        if (remoteHost.request.protocolVersion == 5) {
                            mqtt.wire5.control.packet.PingRequest
                        } else {
                            PingRequest
                        }
                    )
                    delay(keepAliveTimeoutMs)
                } else {
                    delay(keepAliveTimeoutMs - (currentTimestampMs() - lastMessageBetweenClientAndServer()))
                }
            }
        }
    }
}
