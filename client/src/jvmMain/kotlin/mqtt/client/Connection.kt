@file:Suppress("EXPERIMENTAL_API_USAGE")
@file:JvmName("MqttConnection")
package mqtt.client

import io.ktor.network.selector.ActorSelectorManager
import io.ktor.network.sockets.*
import kotlinx.coroutines.*
import kotlinx.coroutines.io.ByteReadChannel
import kotlinx.coroutines.io.ByteWriteChannel
import mqtt.time.currentTimestampMs
import mqtt.wire.data.MqttUtf8String
import mqtt.wire.data.QualityOfService
import mqtt.wire4.control.packet.ConnectionRequest
import mqtt.wire4.control.packet.DisconnectNotification
import mqtt.wire4.control.packet.PublishMessage
import java.net.InetSocketAddress

actual class Connection actual constructor(override val parameters: ConnectionParameters) : AbstractConnection(), Runnable {
    override lateinit var platformSocket: PlatformSocket
    override val dispatcher: CoroutineDispatcher = Dispatchers.IO
    val shutdownThread = Thread(this)

    override fun run() {
        println("got a shutdown message")
        val job = send(DisconnectNotification)
        runBlocking {
            job.join()
            println("disconnect successfully sent!")
        }
    }

    override suspend fun buildSocket(): PlatformSocket {
        val socketBuilder = aSocket(ActorSelectorManager(Dispatchers.IO)).tcp()
        val address = InetSocketAddress(parameters.hostname, parameters.port)
        connectionAttemptTime.lazySet(currentTimestampMs())
        val socket = socketBuilder.connect(address)
        Runtime.getRuntime().addShutdownHook(shutdownThread)
        return JavaPlatformSocket(socket)
    }

    override fun beforeClosingSocket() {
        Runtime.getRuntime().removeShutdownHook(shutdownThread)
    }
}

class JavaPlatformSocket(private val socket: Socket) : PlatformSocket {
    override val output: ByteWriteChannel = socket.openWriteChannel(autoFlush = true)
    override val input: ByteReadChannel = socket.openReadChannel()
    override suspend fun awaitClosed() = socket.awaitClosed()
    override val isClosed: Boolean = socket.isClosed
    override fun dispose() = socket.dispose()
}

fun main() {
    val header = ConnectionRequest.VariableHeader(keepAliveSeconds = 15.toUShort())
    val payload = ConnectionRequest.Payload(clientId = MqttUtf8String("JavaSample"))
    val params = ConnectionParameters("localhost", 1883, ConnectionRequest(header, payload), reconnectIfNetworkLost = false)
    val connection = openConnection(params)
    runBlocking {
        delay(2000)
        val fixed = PublishMessage.FixedHeader(qos = QualityOfService.AT_LEAST_ONCE)
        val variable = PublishMessage.VariableHeader(MqttUtf8String("yolo"), 1.toUShort())
        params.clientToBroker.send(PublishMessage(fixed, variable))
        connection.await()
        println(connection.getCompleted())
    }
}

