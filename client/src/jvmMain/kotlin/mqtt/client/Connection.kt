@file:Suppress("EXPERIMENTAL_API_USAGE")
@file:JvmName("MqttConnection")
package mqtt.client

import io.ktor.network.selector.ActorSelectorManager
import io.ktor.network.sockets.*
import kotlinx.atomicfu.atomic
import kotlinx.coroutines.*
import kotlinx.coroutines.io.writeFully
import kotlinx.io.core.readBytes
import mqtt.time.currentTimestampMs
import mqtt.wire.control.packet.ControlPacket
import mqtt.wire.data.MqttUtf8String
import mqtt.wire.data.QualityOfService
import mqtt.wire4.control.packet.*
import java.net.InetSocketAddress

fun openConnection(parameters: ConnectionParameters) = GlobalScope.async {
    //    return@async if (parameters.reconnectIfNetworkLost) {
//        var oldConnection: Connection
//        retryIO {
//            oldConnection = Connection(parameters)
//            val connection = oldConnection.start()
//            connection.await()
//        }
//        return@async false
//    } else {
    val connection = Connection(parameters)
    return@async connection.start()
//    }
}

actual class Connection actual constructor(override val parameters: ConnectionParameters) : IConnection {
    override var job: Job = Job()
    override val dispatcher: CoroutineDispatcher = Dispatchers.IO
    private val _lastMessageBetweenClientAndServer = atomic(0L)
    override fun lastMessageBetweenClientAndServer() = _lastMessageBetweenClientAndServer.value
    private val _isConnectedOrConnecting = atomic(false)
    override fun isConnectedOrConnecting() = _isConnectedOrConnecting.value

    override fun start() = async {
        if (isConnectedOrConnecting()) {
            println("Already connected or connecting -- start")
            return@async false
        }
        return@async openSocket()
    }

    private suspend fun openSocket(): Boolean {
        if (!_isConnectedOrConnecting.compareAndSet(expect = false, update = true)) {
            println("Already connected or connecting -- open")
            return false
        }

        val socketBuilder = aSocket(ActorSelectorManager(Dispatchers.IO)).tcp()
        val address = InetSocketAddress(parameters.hostname, parameters.port)
        val connectionAttemptTime = currentTimestampMs()
        val socket = socketBuilder.connect(address)
        val shutdownThread = Thread {
            println("got a shutdown message")
            val job = send(DisconnectNotification)
            runBlocking {
                job.join()
                println("disconnect sent!")
            }
        }
        try {
            println("Start connecting $socket")
            val socketConnectedTime = currentTimestampMs()
            val socketConnectionTimeMs = socketConnectedTime - connectionAttemptTime
            println("Connected socket in $socketConnectionTimeMs ms")
            val output = socket.openWriteChannel(autoFlush = true)
            val writeChannelTime = currentTimestampMs()
            val connectionRequestPacket = parameters.connectionRequest.copy().serialize().readBytes()
            val serializationTime = currentTimestampMs()
            val processTime = serializationTime - writeChannelTime
            println("Socket processing time took $processTime ms")
            output.writeFully(connectionRequestPacket)
            val postWriteTime = currentTimestampMs()
            val socketWriteTime = postWriteTime - serializationTime
            println("OUT [${connectionRequestPacket.size}][$socketWriteTime]: ${parameters.connectionRequest}")

            Runtime.getRuntime().addShutdownHook(shutdownThread)
            launch {
                val input = socket.openReadChannel()
                println("open reading channel")
                delay(500)
                val controlPacket = input.read()
                println("read first byte")
                if (controlPacket is ConnectionAcknowledgment) {
                    println("IN: $controlPacket")
                    runKeepAlive()
                }
                while (isActive) {
                    val byte = input.read()
                    println("IN: $byte")
                    _lastMessageBetweenClientAndServer.getAndSet(currentTimestampMs())
                    GlobalScope.launch(Dispatchers.Unconfined) { parameters.brokerToClient.send(controlPacket) }
                }
            }
            for (messageToSend in parameters.clientToBroker) {
                if (isActive || messageToSend is DisconnectNotification) {
                    val sendMessage = messageToSend.serialize()
                    val size = sendMessage.remaining
                    val start = currentTimestampMs()
                    output.writePacket(sendMessage)
                    val writeComplete = currentTimestampMs()
                    _lastMessageBetweenClientAndServer.getAndSet(writeComplete)
                    val sendTime = writeComplete - start
                    println("OUT [$size][$sendTime]: $messageToSend")
                    if (messageToSend is DisconnectNotification) {
                        socket.dispose()
                        println("socket disposed")
                    }
                }
            }
            socket.awaitClosed()
            println("socket closed")
            return true
        } catch (e: CancellationException) {
            return false
        } finally {
            Runtime.getRuntime().removeShutdownHook(shutdownThread)
            println("socket is closed: ${socket.isClosed}")
            val local = isConnectedOrConnecting()
            if (!_isConnectedOrConnecting.compareAndSet(expect = true, update = false)) {
                throw ConcurrentModificationException("Invalid connectivity state")
            }
        }
    }
    private fun runKeepAlive() = launch {
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

    fun send(packet: ControlPacket) = launch {
        parameters.clientToBroker.send(packet)
    }
}


fun main() {
    val header = ConnectionRequest.VariableHeader(keepAliveSeconds = 15.toUShort())
    val payload = ConnectionRequest.Payload(clientId = MqttUtf8String("JavaSample"))
    val params = ConnectionParameters("localhost", 1883, ConnectionRequest(header, payload))
    val connection = Connection(params)
    runBlocking {
        val result = connection.start()
        delay(2000)
        val fixed = PublishMessage.FixedHeader(qos = QualityOfService.AT_MOST_ONCE)
        val variable = PublishMessage.VariableHeader(MqttUtf8String("yolo"))
        connection.send(PublishMessage(fixed, variable))
        result.await()
        println(result.getCompleted())
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
        } catch (e: Exception) {
            // you can log an error here and/or make a more finer-grained
            // analysis of the cause to see if retry is needed
            println("error while retrying")
            e.printStackTrace()
        }
        delay(currentDelay)
        currentDelay = (currentDelay * factor).toLong().coerceAtMost(maxDelay)
    }
    return block() // last attempt
}
