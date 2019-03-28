@file:Suppress("EXPERIMENTAL_API_USAGE")
@file:JvmName("MqttConnection")
package mqtt.client

import io.ktor.network.selector.ActorSelectorManager
import io.ktor.network.sockets.aSocket
import io.ktor.network.tls.tls
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Runnable
import kotlinx.coroutines.runBlocking
import mqtt.time.currentTimestampMs
import mqtt.wire.data.MqttUtf8String
import mqtt.wire.data.QualityOfService
import mqtt.wire4.control.packet.ConnectionRequest
import mqtt.wire4.control.packet.DisconnectNotification
import mqtt.wire4.control.packet.PublishMessage

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
        connectionAttemptTime.lazySet(currentTimestampMs())
        val tmpSocketRef = socketBuilder.connect(parameters.hostname, parameters.port)
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


fun main() {
    val header = ConnectionRequest.VariableHeader(keepAliveSeconds = 5.toUShort())
    val payload = ConnectionRequest.Payload(clientId = MqttUtf8String("JavaSample"))
    val params = ConnectionParameters("test.mosquitto.org", 1883, false,
            ConnectionRequest(header, payload), reconnectIfNetworkLost = false)
    val connection = openConnection(params)
    runBlocking {
        val fixed = PublishMessage.FixedHeader(qos = QualityOfService.AT_LEAST_ONCE)
        val variable = PublishMessage.VariableHeader(MqttUtf8String("yolo"), 1.toUShort())
        params.clientToBroker.send(PublishMessage(fixed, variable))
        connection.await()
    }
}
