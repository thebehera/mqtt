@file:Suppress("EXPERIMENTAL_API_USAGE")

package mqtt.client

import kotlinx.coroutines.Job
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.io.core.buildPacket
import kotlinx.io.core.toByteArray
import kotlinx.io.core.writeFully
import mqtt.client.connection.Closed
import mqtt.client.connection.Open
import mqtt.client.connection.parameters.ConnectionParameters
import mqtt.client.connection.parameters.RemoteHost
import mqtt.client.platform.PlatformCoroutineDispatcher
import mqtt.client.platform.PlatformSocketConnection
import mqtt.client.transport.OnMessageReceivedCallback
import mqtt.time.currentTimestampMs
import mqtt.wire.control.packet.ControlPacket
import mqtt.wire.data.QualityOfService.AT_LEAST_ONCE
import mqtt.wire.data.QualityOfService.AT_MOST_ONCE
import mqtt.wire.data.topic.Filter
import mqtt.wire4.control.packet.*
import kotlin.test.*

/**
 * These tests require running mosquitto servers with the configurations in the ./client/gradle/configurations path
 * of this repo. Or run the gradle `:client:check` or `:client:jvmTest` command with mosquitto installed on the PATH
 */
class SocketTransportTests {

    private val ctx = Job() + PlatformCoroutineDispatcher.dispatcher

    @Test
    fun connectDisconnect() {
        val params = buildParams()
        val connection = PlatformSocketConnection(params, ctx)
        val result = connection.openConnectionAsync(true)
        blockWithTimeout(5000) {
            println("connect startAsync")
            val connectionResult = result.await().value
            assertEquals(Open, connectionResult)
            assertNotNull(connection.connack)

            println("connect close")
            val connectionState = connection.closeAsync().await()

            println("connect close done")
            assertTrue(connectionState)
            val state = connection.state.value
            assertTrue(state is Closed, "$state is not closed")
        }
    }

    @Test
    fun reconnectOnce() {
        val params = buildParams()
        val connection = PlatformSocketConnection(params, ctx)
        val result = connection.openConnectionAsync(true)
        blockWithTimeout {
            result.await()
            connection.closeAsync().await()
            val newParams = params.copy()
            val newConnection = PlatformSocketConnection(newParams, ctx)
            val newResult = newConnection.openConnectionAsync(true)
            newResult.await()
            newConnection.closeAsync().await()
        }
    }

    @Test
    fun socketCloseAutomatically() {
        val params = buildParams()
        val connection = PlatformSocketConnection(params, ctx)
        val result = connection.openConnectionAsync(true)
        block {
            result.await()
        }
    }

    @Test
    fun publishSingleMessageQos0() {
        val params = buildParams()
        val connection = PlatformSocketConnection(params, ctx)
        val result = connection.openConnectionAsync(true)
        blockWithTimeout {
            result.await()
            val publishMessage = PublishMessage("yolo", buildPacket { writeFully("asdf".toByteArray()) })
            connection.clientToServer.send(publishMessage)
            assertEquals(connection.state.value, Open)
        }
    }

    @Test
    fun testQos1() {
        val clientId1 = "Client1"
        val client1Params = buildParams(clientId1)
        val client1SocketConnection1 = PlatformSocketConnection(client1Params, ctx)
        val client2Params = buildParams("Client2")
        val client2SocketConnection = PlatformSocketConnection(client2Params, ctx)


        val subscribe = SubscribeRequest(Filter("test"), AT_LEAST_ONCE)
        val publish = PublishMessage("test", AT_LEAST_ONCE, buildPacket { writeInt(1) }, retain = true)
        blockWithTimeout {
            client1SocketConnection1.openConnectionAsync().await()
            client2SocketConnection.openConnectionAsync().await()

            println("CLIENT 1 SENDING SUBSCRIBE")
            client1SocketConnection1.clientToServer.send(subscribe)
            println("CLIENT 2 SENDING PUBLISH")
            client2SocketConnection.clientToServer.send(publish)
//                client1SocketConnection1.cancel()
//                delay(5000)
//                val client1SocketConnection2 = PlatformSocketConnection(client2Params.copy())
//                println("CLIENT 1 S2 CONNECT")
//                client1SocketConnection2.openConnectionAsync().await()
////                client1SocketConnection2.clientToServer.send(subscribe.copy())
//                delay(20000)
//                mutex.withLock {} // lock until we get a message
        }
    }

    fun buildParams(clientId: String = getClientId()): ConnectionParameters {
        val connectionRequest = ConnectionRequest(clientId = clientId, keepAliveSeconds = 5000.toUShort())
        return ConnectionParameters(
            RemoteHost(
                domain,
                port = port,
                request = connectionRequest,
                security = RemoteHost.Security(
                    isTransportLayerSecurityEnabled = false
                ),
                maxNumberOfRetries = 3
            )
        )
    }

    suspend fun buildConnection(clientId: String = getClientId()): PlatformSocketConnection {
        val params = buildParams(clientId)
        val connection = PlatformSocketConnection(params, ctx)
        connection.openConnectionAsync().await()
        return connection
    }

    @Test
    fun publishQos1() {
        var recvMessage = false
        blockWithTimeout(5000) {
            val publishClient = buildConnection("pubClient")
            val recvClientSession1 = buildConnection("client1")
            val subscribe = SubscribeRequest(Filter("test"), AT_LEAST_ONCE)
            recvClientSession1.clientToServer.send(subscribe)
            recvClientSession1.closeAsync().await()

            val publish = PublishMessage("test", AT_LEAST_ONCE, buildPacket { writeInt(1) })
            publishClient.clientToServer.send(publish)
            val mutex = Mutex(true)
            val recvClientSession2Params = buildParams("client1")
            val recvClientSession2Connection = PlatformSocketConnection(recvClientSession2Params, ctx)
            recvClientSession2Connection.messageReceiveCallback = object : OnMessageReceivedCallback {
                override fun onMessage(controlPacket: ControlPacket) {
                    try {
                        if (controlPacket is ConnectionAcknowledgment) {
                            return
                        }
                        if (controlPacket !is PublishMessage) {
                            fail("received wrong message: $controlPacket")
                        }
                        if (!mutex.isLocked || recvMessage) {
                            return
                        }
                        recvMessage = true
                        mutex.unlock()
                    } catch (e: Exception) {
                        fail(e.message)
                    }
                }
            }
            val job = recvClientSession2Connection.openConnectionAsync()
            job.await()
            mutex.withLock {} // lock until we get a message
            assertTrue(recvMessage)
            recvClientSession2Connection.closeAsync().await()

        }
    }

    @Test
    fun subscribeAndReceiveSuback() {
        val params = buildParams()
        val connection = PlatformSocketConnection(params, ctx)
        val result = connection.openConnectionAsync(true)
        var recvMessage = false
        blockWithTimeout {
            result.await()
            println("awaited result")
            val mutex = Mutex(true)
            connection.messageReceiveCallback = object : OnMessageReceivedCallback {
                override fun onMessage(controlPacket: ControlPacket) {
                    try {
                        assertTrue(controlPacket is SubscribeAcknowledgement)
                        recvMessage = true
                        println("unlock")
                        mutex.unlock()
                    } catch (e: Exception) {
                        fail(e.message)
                    }
                }
            }
            val subscribeRequest = SubscribeRequest(19.toUShort(), listOf(Subscription(Filter("yolo"), AT_MOST_ONCE)))
            connection.clientToServer.send(subscribeRequest)
            mutex.withLock {} // lock until we get a message
            assertTrue(recvMessage)

        }
    }
}

fun getClientId(name: String? = null): String {
    val randomString = if (name == null) {
        val charPool: List<Char> = ('a'..'z') + ('A'..'Z') + ('0'..'9')
        (1..10)
                .map { i -> kotlin.random.Random.nextInt(0, charPool.size) }
                .map(charPool::get)
                .joinToString("")
    } else {
        name
    }
    return "MqttClientTests${currentTimestampMs()}$randomString"
}