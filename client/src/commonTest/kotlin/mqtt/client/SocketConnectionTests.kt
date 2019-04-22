@file:Suppress("EXPERIMENTAL_API_USAGE")

package mqtt.client

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withTimeout
import kotlinx.io.core.buildPacket
import kotlinx.io.core.toByteArray
import kotlinx.io.core.writeFully
import mqtt.time.currentTimestampMs
import mqtt.wire.data.QualityOfService.AT_MOST_ONCE
import mqtt.wire.data.topic.Filter
import mqtt.wire4.control.packet.*
import kotlin.test.*

class SocketConnectionTests {

    fun getClientId(): String {
        val charPool: List<Char> = ('a'..'z') + ('A'..'Z') + ('0'..'9')
        val randomString = (1..10)
                .map { i -> kotlin.random.Random.nextInt(0, charPool.size) }
                .map(charPool::get)
                .joinToString("")
        return "MqttClientTests${currentTimestampMs()}_$randomString"
    }
    @Test
    fun connectDisconnect() {
        val connectionRequest = ConnectionRequest(clientId = getClientId(), keepAliveSeconds = 5.toUShort())
        val params = ConnectionParameters("localhost", 60000, false, connectionRequest)
        val connection = PlatformSocketConnection(params)
        val result = connection.openConnectionAsync(true)
        block {
            withTimeout(5000) {
                println("connect start")
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
    }

    @Test
    fun reconnectOnce() {
        val connectionRequest = ConnectionRequest(clientId = getClientId(), keepAliveSeconds = 5.toUShort(), cleanSession = true)
        val params = ConnectionParameters("localhost", 60000, false, connectionRequest)
        val connection = PlatformSocketConnection(params)
        val result = connection.openConnectionAsync(true)
        block {
            withTimeout(5000) {
                result.await()
                connection.closeAsync().await()
                val newParams = params.copy()
                val newConnection = PlatformSocketConnection(newParams)
                val newResult = newConnection.openConnectionAsync(true)
                newResult.await()
                newConnection.closeAsync().await()
            }
        }
    }

    @Test
    fun socketCloseAutomatically() {
        val connectionRequest = ConnectionRequest(clientId = getClientId(), keepAliveSeconds = 5.toUShort())
        val params = ConnectionParameters("localhost", 60000, false, connectionRequest)
        val connection = PlatformSocketConnection(params)
        val result = connection.openConnectionAsync(true)
        block {
            result.await()
        }
    }

    @Test
    fun publishSingleMessageQos0() {
        val connectionRequest = ConnectionRequest(clientId = getClientId(), keepAliveSeconds = 5.toUShort())
        val params = ConnectionParameters("localhost", 60000, false, connectionRequest)
        val connection = PlatformSocketConnection(params)
        val result = connection.openConnectionAsync(true)
        block {
            result.await()
            val publishMessage = PublishMessage("yolo", buildPacket { writeFully("asdf".toByteArray()) })
            connection.clientToServer.send(publishMessage)
            assertEquals(connection.state.value, Open)
        }
    }

    @Test
    fun subscribeAndReceiveSuback() {
        val connectionRequest = ConnectionRequest(clientId = getClientId(), keepAliveSeconds = 5000.toUShort())
        val params = ConnectionParameters("localhost", 60000, false, connectionRequest)
        val connection = PlatformSocketConnection(params)
        val result = connection.openConnectionAsync(true)
        var recvMessage = false
        block {
            withTimeout(5000) {
                result.await()
                val mutex = Mutex(true)
                launch(Dispatchers.Unconfined) {
                    try {
                        val controlPacket = connection.serverToClient.receive()
                        assertTrue(controlPacket is SubscribeAcknowledgement)
                        recvMessage = true
                        mutex.unlock()
                    } catch (e: Exception) {
                        fail(e.message)
                    }
                }
                val subscribeRequest = SubscribeRequest(19.toUShort(), listOf(Subscription(Filter("yolo"), AT_MOST_ONCE)))
                connection.clientToServer.send(subscribeRequest)
                mutex.withLock {} // lock until we get a message
                assertTrue(recvMessage)
            }
        }
    }
}
