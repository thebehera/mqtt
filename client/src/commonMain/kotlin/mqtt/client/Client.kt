package mqtt.client

import kotlinx.coroutines.*
import kotlinx.coroutines.channels.BroadcastChannel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.collect
import mqtt.client.persistence.InMemoryPersistence
import mqtt.client.persistence.Persistence
import mqtt.client.socket.ConnectionState
import mqtt.client.socket.ISocketController
import mqtt.client.socket.SocketController
import mqtt.client.websocket.WebsocketController
import mqtt.connection.IConnectionOptions
import mqtt.wire.buffer.GenericType
import mqtt.wire.control.packet.*
import mqtt.wire.control.packet.RetainHandling.SEND_RETAINED_MESSAGES_AT_TIME_OF_SUBSCRIBE
import mqtt.wire.control.packet.format.ReasonCode
import mqtt.wire.data.QualityOfService
import mqtt.wire.data.QualityOfService.*
import mqtt.wire.data.topic.Filter
import kotlin.coroutines.cancellation.CancellationException
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine
import kotlin.time.ExperimentalTime
import kotlin.time.TimeSource
import kotlin.time.milliseconds
import kotlin.time.seconds

@ExperimentalTime
@ExperimentalUnsignedTypes
class Client(
    private val connectionOptions: IConnectionOptions,
    private val messageCallback: ApplicationMessageCallback? = null,
    private val persistence: Persistence = InMemoryPersistence(),
    private val scope: CoroutineScope,
    private val enableKeepAlive: Boolean = true
) {
    private val packetFactory = connectionOptions.request.controlPacketFactory
    private var socketController: ISocketController? = null
    private var reconnectCount = 0
    private var keepAliveJob: Job? = null
    private var incomingMessageBroadcastChannel = BroadcastChannel<ControlPacket>(Channel.BUFFERED)

    var authenticator: ((ControlPacket) -> ControlPacket)? = null

    var connectionState: ConnectionState = ConnectionState.Disconnected
        set(value) {
            field = value
            onConnectionStateCallback?.invoke(value)
        }

    var onConnectionStateCallback: ((ConnectionState) -> Unit)? = null
    private fun isConnected() = connectionState is ConnectionState.Connected

    suspend fun connect(): IConnectionAcknowledgment{
        reconnectCount++
        val request = connectionOptions.request
//        if (request.cleanStart) {
//            persistence.clear()
//        }
        val websocketParams = connectionOptions.websocketEndpoint
        socketController = if (websocketParams != null) {
            WebsocketController.openWebSocket(scope, connectionOptions)!!
        } else {
            val controller = SocketController.openSocket(scope, connectionOptions)
                ?: throw UnsupportedOperationException("Native sockets area not supportd. make sure you have added the websocket params to the IRemoteHost object")
            controller
        }
        val socketController = socketController!!
        socketController.write(request)
        messageCallback?.onMessageSent(request)
        scope.launch {
            routeIncomingMessages()
        }
        return suspendUntilMessage { it is IConnectionAcknowledgment } as IConnectionAcknowledgment
    }

    suspend fun disconnectAsync(
        reasonCode: ReasonCode = ReasonCode.NORMAL_DISCONNECTION,
        sessionExpiryIntervalSeconds: Long? = null,
        reasonString: CharSequence? = null,
        userProperty: List<Pair<CharSequence, CharSequence>> = emptyList(),
        serverReference: CharSequence? = null
    ) = scope.async {
        if (keepAliveJob?.isActive == true) {
            keepAliveJob?.cancel()
        }
        keepAliveJob = null
        val socketControllerTmp = socketController
        socketController = null
        val disconnect = packetFactory.disconnect(
            reasonCode,
            sessionExpiryIntervalSeconds,
            reasonString,
            userProperty,
            serverReference
        )
        socketControllerTmp?.write(disconnect)
        messageCallback?.onMessageSent(disconnect)
        launch {
            // just add a short delay to prevent things from crashing
            delay(10)
            socketControllerTmp?.close()
        }
        disconnect
    }

    fun stayConnectedAsync() = scope.launch(Dispatchers.Default) {
        val exponentialBackoffFactor = 2.0
        val maxDelay = 10.seconds
        val initialFailureDelay = 1.seconds
        var currentDelay = initialFailureDelay
        while (isActive) {
            var currentException :Throwable? = null
            try {
                val result = connect()
                currentDelay = initialFailureDelay
                val socketController = socketController!!
                connectionState = ConnectionState.Connected(result, socketController)
                println("connected $connectionState")
                suspendCoroutine<Unit> {
                    socketController.closedSocketCallback = {
                        println("socket closed callback")
                        socketController.closedSocketCallback = null
                        keepAliveJob?.cancel()
                        keepAliveJob = null
                        it.resume(Unit)
                    }
                }
                println("start reconnecting")
                connectionState = ConnectionState.Reconnecting(null, currentDelay)
            } catch (e: Exception) {
                currentException = e
                connectionState = ConnectionState.Reconnecting(e, currentDelay)
            }
            keepAliveJob?.cancel(CancellationException(currentException))
            keepAliveJob = null
            socketController?.closedSocketCallback = null
            socketController?.close()
            socketController = null
            println("disconnected $currentDelay $isActive")

            delay(currentDelay)
            println("undelay $isActive")
            currentDelay = (currentDelay * exponentialBackoffFactor)
            if (currentDelay > maxDelay) currentDelay = maxDelay
        }
    }

    fun subscribeAsync(
        subscription: CharSequence,
        maxQos: QualityOfService = EXACTLY_ONCE,
        // The rest of these properties are used by mqtt 5
        noLocal: Boolean = false,
        retainAsPublished: Boolean = false,
        retainHandling: RetainHandling = SEND_RETAINED_MESSAGES_AT_TIME_OF_SUBSCRIBE,
        reasonString: CharSequence? = null,
        userProperty: List<Pair<CharSequence, CharSequence>> = emptyList()
    ) =
        subscribeAsync(
            setOf(SubscriptionWrapper(Filter(subscription), maxQos, noLocal, retainAsPublished, retainHandling)),
            reasonString,
            userProperty
        )

    fun subscribeAsync(
        subscriptions: Set<SubscriptionWrapper>,
        reasonString: CharSequence? = null,
        userProperty: List<Pair<CharSequence, CharSequence>> = emptyList()
    ) = scope.async {
        val packetIdentifier = persistence.leasePacketIdentifier(packetFactory).toInt()
        val sub = packetFactory.subscribe(packetIdentifier, subscriptions, reasonString, userProperty)
        routeOutgoingMessages(sub)
        suspendUntilMessage { it is ISubscribeAcknowledgement && it.packetIdentifier == packetIdentifier }
    }

    fun unsubscribeAsync(vararg subscriptions: CharSequence) = scope.async {
        val packetIdentifier = persistence.leasePacketIdentifier(packetFactory).toInt()
        val unsub = packetFactory.unsubscribe(packetIdentifier, subscriptions.toHashSet())
        routeOutgoingMessages(unsub)
        suspendUntilMessage { it is IUnsubscribeAckowledgment && it.packetIdentifier == packetIdentifier }
    }


    fun publishAsync(
        topicName: CharSequence,
        payload: String? = null,
        qos: QualityOfService = AT_LEAST_ONCE,
        dup: Boolean = false,
        retain: Boolean = false,
        // MQTT 5 Properties
        payloadFormatIndicator: Boolean = false,
        messageExpiryInterval: Long? = null,
        topicAlias: Int? = null,
        responseTopic: CharSequence? = null,
        correlationData: String? = null,
        userProperty: List<Pair<CharSequence, CharSequence>> = emptyList(),
        subscriptionIdentifier: Set<Long> = emptySet(),
        contentType: CharSequence? = null
    ) = publishAsync(
        topicName,
        payload?.let { GenericType(it, String::class) },
        qos,
        dup,
        retain,
        payloadFormatIndicator,
        messageExpiryInterval,
        topicAlias,
        responseTopic,
        correlationData?.let { GenericType(it, String::class) },
        userProperty,
        subscriptionIdentifier,
        contentType
    )

    fun <ApplicationMessage : Any, CorrelationData : Any> publishAsync(
        topicName: CharSequence,
        payload: GenericType<ApplicationMessage>? = null,
        qos: QualityOfService = AT_LEAST_ONCE,
        dup: Boolean = false,
        retain: Boolean = false,
        // MQTT 5 Properties
        payloadFormatIndicator: Boolean = false,
        messageExpiryInterval: Long? = null,
        topicAlias: Int? = null,
        responseTopic: CharSequence? = null,
        correlationData: GenericType<CorrelationData>? = null,
        userProperty: List<Pair<CharSequence, CharSequence>> = emptyList(),
        subscriptionIdentifier: Set<Long> = emptySet(),
        contentType: CharSequence? = null
    ) = scope.async {
        val packetIdentifier = if (qos.isGreaterThan(AT_MOST_ONCE)) {
            persistence.leasePacketIdentifier(packetFactory).toInt()
        } else null
        val pub = packetFactory.publish(
            dup,
            qos,
            packetIdentifier,
            retain,
            topicName,
            payload,
            payloadFormatIndicator,
            messageExpiryInterval,
            topicAlias,
            responseTopic,
            correlationData,
            userProperty,
            subscriptionIdentifier,
            contentType
        )
        routeOutgoingMessages(pub)
        when (pub.qualityOfService) {
            AT_LEAST_ONCE -> {
                suspendUntilMessage { it is IPublishAcknowledgment && it.packetIdentifier == packetIdentifier }
            }
            EXACTLY_ONCE -> {
                suspendUntilMessage { it is IPublishReceived && it.packetIdentifier == packetIdentifier }
                suspendUntilMessage { it is IPublishComplete && it.packetIdentifier == packetIdentifier }
            }
            else -> Unit
        }
    }

    private suspend fun routeOutgoingMessages(
        outgoingControlPacket: ControlPacket
    ) {
        when (outgoingControlPacket) {
            is IPublishMessage -> {
                if (outgoingControlPacket.qualityOfService.isGreaterThan(AT_MOST_ONCE)) {
                    persistence.storeOutgoing(outgoingControlPacket)
                }
                socketController?.write(outgoingControlPacket)
            }
            is ISubscribeRequest -> {
                persistence.subscribe(outgoingControlPacket)
                socketController?.write(outgoingControlPacket)
            }
            is IUnsubscribeRequest -> {
                persistence.unsubscribe(outgoingControlPacket)
                socketController?.write(outgoingControlPacket)
            }
            is IDisconnectNotification -> {
                socketController?.write(outgoingControlPacket)
                socketController?.close()
            }
            else -> socketController?.write(outgoingControlPacket)
        }
        if (socketController != null) {
            messageCallback?.onMessageSent(outgoingControlPacket)
        }
    }

    private suspend fun getExpectedResponse(pub: IPublishMessage):
            ControlPacket? {
        val mqtt5Extras = try {
            messageCallback?.onPublishMessageReceived5(this, pub)
        } catch (e: Exception) {
            ApplicationMessageCallback.Mqtt5Extras(
                ReasonCode.UNSPECIFIED_ERROR,
                "${e.message ?: "No Exception Message"}\r\n${e.stackTraceToString()}"
            )
        }
        return if (mqtt5Extras != null) {
            pub.expectedResponse(mqtt5Extras.reasonCode, mqtt5Extras.reasonString, mqtt5Extras.userProperty)
        } else {
            pub.expectedResponse()
        }
    }

    private suspend fun routeIncomingMessages() {
        var receivedConnack = false
        socketController?.read()?.collect { incomingControlPacket ->
            when (incomingControlPacket) {
                is IPublishMessage -> {
                    when (incomingControlPacket.qualityOfService) {
                        AT_MOST_ONCE -> {
                            messageCallback?.onPublishMessageReceived5(this, incomingControlPacket)
                        }
                        AT_LEAST_ONCE -> {
                            routeOutgoingMessages(getExpectedResponse(incomingControlPacket)!!)
                        }
                        EXACTLY_ONCE -> {
                            if (persistence.storeIncoming(incomingControlPacket.packetIdentifier!!)) {
                                routeOutgoingMessages(getExpectedResponse(incomingControlPacket)!!)
                            }
                        }
                    }
                }
                is IPublishAcknowledgment -> {
                    persistence.ack(incomingControlPacket)
                }
                is IPublishReceived -> {
                    val pubRelResponse = incomingControlPacket.expectedResponse()
                    persistence.received(pubRelResponse)
                    routeOutgoingMessages(pubRelResponse)
                }
                is IPublishRelease -> {
                    val pubcompResponse = incomingControlPacket.expectedResponse()
                    persistence.release(pubcompResponse)
                    routeOutgoingMessages(pubcompResponse)
                }
                is IPublishComplete -> {
                    persistence.complete(incomingControlPacket)
                }
                is ISubscribeAcknowledgement -> {
                    persistence.acknowledge(incomingControlPacket)
                    messageCallback?.onAcknowledgementReceived(incomingControlPacket)
                }
                is IUnsubscribeAckowledgment -> {
                    persistence.acknowledge(incomingControlPacket)
                    messageCallback?.onAcknowledgementReceived(incomingControlPacket)
                }
                is IConnectionAcknowledgment -> {
                    if (!connectionOptions.request.cleanStart) {
                        val queuedMessages = persistence.queuedMessages()
                        if (queuedMessages.isNotEmpty()) {
                            socketController?.write(queuedMessages.values)
                        }
                    }
                    reconnectCount = 0
                    receivedConnack = true
                    if (enableKeepAlive) {
                        maintainKeepAlive()
                    }
                }
            }

            val authenticator = authenticator
            if (!receivedConnack && connectionState !is ConnectionState.Connected && authenticator != null) {
                socketController?.write(authenticator(incomingControlPacket))
            } else if (!receivedConnack && connectionState !is ConnectionState.Connected && authenticator == null) {
                throw IllegalStateException("Requires an authenticator for $connectionOptions but failed to find one")
            }
            incomingMessageBroadcastChannel.send(incomingControlPacket)
            messageCallback?.onMessageReceived(incomingControlPacket)
        }
    }

    suspend fun writePing() :Boolean {
        val result = (socketController?.write(packetFactory.pingRequest()) ?: -1) != 0
        if (result) {
            messageCallback?.onMessageSent(packetFactory.pingRequest())
        }
        return result
    }

    private fun maintainKeepAlive() {
        keepAliveJob?.cancel()
        keepAliveJob = scope.launch {
            try {
                while (isActive && isConnected() && socketController != null) {
                    if (!writePing()) {
                        return@launch
                    }
                    socketController?.lastMessageSent = TimeSource.Monotonic.markNow()
                    delay(connectionOptions.request.keepAliveTimeout.div(2))
                }
            } catch (e: Throwable) {
                // ignore cancellation exceptions
            }
        }
    }

    private suspend fun suspendUntilMessage(matches: (ControlPacket) -> Boolean = {true}): ControlPacket {
        val coroutineSubscription = incomingMessageBroadcastChannel.openSubscription()
        var done = false
        while (!done) {
            val packet = coroutineSubscription.receive()
            done = matches(packet)
            if (done) {
                coroutineSubscription.cancel()
                return packet
            }
        }
        throw IllegalStateException("Impossible state when waiting for message!")
    }

}