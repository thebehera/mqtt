@file:Suppress("EXPERIMENTAL_API_USAGE")
@file:JvmName("MqttConnection")
package mqtt.client

import io.ktor.network.selector.ActorSelectorManager
import io.ktor.network.sockets.*
import io.ktor.network.tls.tls
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Runnable
import kotlinx.coroutines.io.ByteReadChannel
import kotlinx.coroutines.io.ByteWriteChannel
import kotlinx.coroutines.runBlocking
import mqtt.time.currentTimestampMs
import mqtt.wire.data.MqttUtf8String
import mqtt.wire4.control.packet.ConnectionRequest
import mqtt.wire4.control.packet.DisconnectNotification
import java.net.InetSocketAddress

actual class Connection actual constructor(override val parameters: ConnectionParameters) : AbstractConnection(), Runnable {
    override lateinit var platformSocket: PlatformSocket
    override val dispatcher: CoroutineDispatcher = Dispatchers.IO
    val shutdownThread = Thread(this)

    override fun run() {
        val job = send(DisconnectNotification)
        runBlocking {
            job.join()
        }
    }

    override suspend fun buildSocket(): PlatformSocket {
        val socketBuilder = aSocket(ActorSelectorManager(Dispatchers.IO)).tcp()
        val address = InetSocketAddress(parameters.hostname, parameters.port)
        connectionAttemptTime.lazySet(currentTimestampMs())
        val tmpSocketRef = socketBuilder.connect(address)
        val socket = if (parameters.secure) {
            tmpSocketRef.tls(coroutineContext)
        } else {
            tmpSocketRef
        }
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
    val header = ConnectionRequest.VariableHeader(keepAliveSeconds = 5.toUShort(), hasUserName = true, hasPassword = true)
    val payload = ConnectionRequest.Payload(clientId = MqttUtf8String("JavaSample"), userName = MqttUtf8String("hktkqtso"), password = MqttUtf8String("C39Mn5EQYQQZ"))
    val params = ConnectionParameters("m16.cloudmqtt.com", 22655, true, ConnectionRequest(header, payload), reconnectIfNetworkLost = true)
//    val header = ConnectionRequest.VariableHeader(keepAliveSeconds = 5.toUShort())
//    val payload = ConnectionRequest.Payload(clientId = MqttUtf8String("JavaSample"))
//    val params = ConnectionParameters("localhost", 1883,false, ConnectionRequest(header, payload), reconnectIfNetworkLost = true)

    val connection = openConnection(params)
    runBlocking {
        //        val fixed = PublishMessage.FixedHeader(qos = QualityOfService.AT_LEAST_ONCE)
//        val variable = PublishMessage.VariableHeader(MqttUtf8String("yolo"), 1.toUShort())
//        delay(1000)
//        params.clientToBroker.send(PublishMessage(fixed, variable))
//        params.clientToBroker.send(DisconnectNotification)
        for (inMessage in params.brokerToClient) {
            println("IN: $inMessage")
        }
        connection.await()
        println("Completion: ${connection.getCompleted()}")
    }
}

