@file:Suppress("EXPERIMENTAL_API_USAGE")
@file:JvmName("MqttConnection")
package mqtt.client

import io.ktor.network.selector.ActorSelectorManager
import io.ktor.network.sockets.aSocket
import io.ktor.network.sockets.awaitClosed
import io.ktor.network.sockets.openReadChannel
import io.ktor.network.sockets.openWriteChannel
import kotlinx.atomicfu.atomic
import kotlinx.coroutines.*
import kotlinx.coroutines.io.writeFully
import kotlinx.coroutines.selects.select
import kotlinx.io.core.readBytes
import mqtt.time.currentTimestampMs
import mqtt.wire.control.packet.ControlPacket
import mqtt.wire.data.MqttUtf8String
import mqtt.wire.data.QualityOfService
import mqtt.wire4.control.packet.*
import java.net.InetSocketAddress

actual class Connection actual constructor(override val parameters: ConnectionParameters) : IConnection {
    override var job: Job = Job()
    override val dispatcher: CoroutineDispatcher = Dispatchers.IO
    private val _lastMessageBetweenClientAndServer get() = atomic(0L)
    override fun lastMessageBetweenClientAndServer() = _lastMessageBetweenClientAndServer.value
    private val _isConnectedOrConnecting get() = atomic(false)
    override fun isConnectedOrConnecting() = _isConnectedOrConnecting.value

    override fun start() = launch {
        if (_isConnectedOrConnecting.value) {
            println("Already connected or connecting -- start")
            return@launch
        }
        if (parameters.reconnectIfNetworkLost) {
            stayConnected()
        } else {
            openSocket()
        }
    }

    private suspend fun openSocket() {
        if (!_isConnectedOrConnecting.compareAndSet(expect = false, update = true)) {
            println("Already connected or connecting -- open")
            resetJob()
            return
        }
        try {
            println("Start connecting")
            val socketBuilder = aSocket(ActorSelectorManager(Dispatchers.IO)).tcp()
            val address = InetSocketAddress(parameters.hostname, parameters.port)
            val connectionAttemptTime = currentTimestampMs()
            val socket = socketBuilder.connect(address)
            val socketConnectedTime = currentTimestampMs()
            val socketConnectionTimeMs = socketConnectedTime - connectionAttemptTime
            println("Connected socket in $socketConnectionTimeMs ms")
            val output = socket.openWriteChannel(autoFlush = true)
            val writeChannelTime = currentTimestampMs()
            val connectionRequestPacket = parameters.connectionRequest.serialize().readBytes()
            val serializationTime = currentTimestampMs()
            val processTime = serializationTime - writeChannelTime
            println("Socket processing time took $processTime ms")
            output.writeFully(connectionRequestPacket)
            val postWriteTime = currentTimestampMs()
            val socketWriteTime = postWriteTime - serializationTime
            println("OUT [${connectionRequestPacket.size}][$socketWriteTime]: ${parameters.connectionRequest}")


            launch {
                val input = socket.openReadChannel()
                val initialControlPacket = input.read()
                val messageReadTime = currentTimestampMs()
                _lastMessageBetweenClientAndServer.lazySet(messageReadTime)
                if (initialControlPacket is ConnectionAcknowledgment) {
                    val postConnackTime = currentTimestampMs()
                    println("Connected in ${postConnackTime - postWriteTime} ms")
                    runKeepAlive()
                }
                while (isActive) {
                    val controlPacket = input.read()
                    _lastMessageBetweenClientAndServer.lazySet(currentTimestampMs())
                    parameters.brokerToClient.send(controlPacket)
                }
            }
            for (messageToSend in parameters.clientToBroker) {
                if (isActive || messageToSend is DisconnectNotification) {
                    val sendMessage = messageToSend.serialize()
                    val size = sendMessage.remaining
                    val start = currentTimestampMs()
                    output.writePacket(sendMessage)
                    val writeComplete = currentTimestampMs()
                    _lastMessageBetweenClientAndServer.lazySet(writeComplete)
                    val sendTime = writeComplete - start
                    println("OUT [$size][$sendTime]: $messageToSend")
                    if (messageToSend is DisconnectNotification) {
                        @Suppress("BlockingMethodInNonBlockingContext") // not sure what to do here
                        socket.close()
                        resetJob()
                    }
                }
            }
            socket.awaitClosed()
        } finally {
            if (!_isConnectedOrConnecting.compareAndSet(expect = true, update = false)) {
                throw ConcurrentModificationException("Invalid connectivity state")
            }
            resetJob()
        }
    }

    suspend fun resetJob() {
        select<Unit> {
            cancel()
            job = Job()
        }
    }

    private fun runKeepAlive() = launch {
        val keepAliveTimeoutSeconds = parameters.connectionRequest.keepAliveTimeoutSeconds.toLong()
        if (keepAliveTimeoutSeconds > 0) {
            val keepAliveTimeoutMs = keepAliveTimeoutSeconds * 1000
            while (isActive) {
                val timeLeftMs = _lastMessageBetweenClientAndServer.value - currentTimestampMs()
                if (timeLeftMs > keepAliveTimeoutMs) {
                    delay(timeLeftMs)
                    continue
                } else {
                    parameters.clientToBroker.send(PingRequest)
                    delay(keepAliveTimeoutMs)
                }
            }
        }
    }

    fun send(packet: ControlPacket) = async {
        parameters.clientToBroker.send(packet)
    }

    suspend fun stayConnected(
            times: Int = Int.MAX_VALUE - 1,
            initialDelay: Long = 1,   // 0.001 second
            maxDelay: Long = 1000,    // 1 second
            factor: Double = 2.0) {
        var currentDelay = initialDelay
        repeat(times) {
            if (!isActive) {
                return
            }
            try {
                openSocket()
            } catch (e: Exception) {
                println("Error while trying to connect: $e")
            }
            delay(currentDelay)
            currentDelay = (currentDelay * factor).toLong().coerceAtMost(maxDelay)
        }
    }
}


fun main() {
    val header = ConnectionRequest.VariableHeader(
            protocolLevel = 4.toUByte(),
            keepAliveSeconds = 2.toUShort(),
            cleanSession = true,
            willQos = QualityOfService.AT_MOST_ONCE)
    val payload = ConnectionRequest.Payload(clientId = MqttUtf8String("thebehera163224626"))
    val params = ConnectionParameters("localhost", 1883, ConnectionRequest(header, payload))
    val connection = Connection(params)
    connection.start()
    val runBlocking = runBlocking {
        delay(1000)
        val fixed = PublishMessage.FixedHeader(qos = QualityOfService.AT_LEAST_ONCE)
        val variable = PublishMessage.VariableHeader(MqttUtf8String("yolo"), 5.toUShort())
        val sent = connection.send(PublishMessage(fixed, variable))
        println(sent)
        delay(5000)
        connection.send(DisconnectNotification)
    }
    println(runBlocking)
}
