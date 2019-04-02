package mqtt.client

import io.ktor.network.selector.ActorSelectorManager
import io.ktor.network.sockets.aSocket
import io.ktor.network.tls.tls
import kotlinx.coroutines.*
import mqtt.wire.data.MqttUtf8String
import mqtt.wire4.control.packet.ConnectionRequest

internal actual class PlatformSocketConnection(override val parameters: ConnectionParameters) : AbstractSocketConnection() {
    override val dispatcher: CoroutineDispatcher = Dispatchers.IO

    override suspend fun buildSocket(): PlatformSocket {
        @Suppress("EXPERIMENTAL_API_USAGE")
        val socketBuilder = aSocket(ActorSelectorManager(Dispatchers.IO)).tcp()
        val tmpSocketRef = socketBuilder.connect(parameters.hostname, parameters.port)
        val socket = if (parameters.secure) {
            tmpSocketRef.tls(coroutineContext)
        } else {
            tmpSocketRef
        }
        shutdownThread.addConnection(this)
        return JavaPlatformSocket(socket)
    }

    override fun beforeClosingSocket() {
        shutdownThread.removeConnections(this)
    }
}

private val shutdownThread by lazy { ShutdownMqttThread() }

internal class ShutdownMqttThread : Thread("MQTT Global Connection Shutdown Hook, clean disconnecting clients") {
    private val connections = HashSet<PlatformSocketConnection>()

    fun addConnection(socketConnection: PlatformSocketConnection) {
        if (connections.size == 0) {
            Runtime.getRuntime().addShutdownHook(this)
        }
        connections += socketConnection
    }

    override fun run() {
        println("Shut down received, closing ${connections.size} connections")
        val jobs = mutableListOf<Job>()
        connections.forEach { jobs += it.closeAsync() }
        runBlocking {
            jobs.forEach { it.join() }
        }
        println("Successfully shut down connections")
    }

    fun removeConnections(socketConnection: PlatformSocketConnection) {
        connections -= socketConnection
        if (connections.size == 0) {
            try {
                Runtime.getRuntime().removeShutdownHook(this)
            } catch (e: IllegalStateException) {
                // ignore because we are shutting down
            }
        }
    }
}


fun main() {
    val header = ConnectionRequest.VariableHeader(keepAliveSeconds = 5.toUShort())
    val payload = ConnectionRequest.Payload(clientId = MqttUtf8String("JavaSample"))
    val params = ConnectionParameters("test.mosquitto.org", 1883, false,
            ConnectionRequest(header, payload), reconnectIfNetworkLost = false)

    val connection = PlatformSocketConnection(params)
    val result = connection.openConnectionAsync(true)
    runBlocking {
        val connectionResult = result.await().value
        println("Connection state: $connectionResult")
        delay(10000)
        println("current state before shutting down " + connection.state.value)
    }
}
