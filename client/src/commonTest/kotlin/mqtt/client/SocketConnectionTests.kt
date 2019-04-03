@file:Suppress("EXPERIMENTAL_API_USAGE")

package mqtt.client

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withTimeout
import kotlinx.io.core.toByteArray
import mqtt.time.currentTimestampMs
import mqtt.wire.data.MqttUtf8String
import mqtt.wire.data.QualityOfService.AT_MOST_ONCE
import mqtt.wire4.control.packet.*
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class SocketConnectionTests {

    fun getClientId(): String {
        return "MqttClientTests${currentTimestampMs()}"
    }
    @Test
    fun connectDisconnect() {
        val connectionRequest = ConnectionRequest(clientId = getClientId(), keepAliveSeconds = 5.toUShort())
        val params = ConnectionParameters("test.mosquitto.org", 1883, false, connectionRequest)
        val connection = PlatformSocketConnection(params)
        val result = connection.openConnectionAsync(true)
        block {
            withTimeout(5000) {
                val connectionResult = result.await().value
                assertEquals(Open, connectionResult)
                assertNotNull(connection.connack)
                val connectionState = connection.closeAsync().await()
                assertTrue(connectionState)
                val state = connection.state.value
                assertTrue(state is Closed)
            }
        }
    }

    @Test
    fun reconnectOnce() {
        val connectionRequest = ConnectionRequest(clientId = getClientId(), keepAliveSeconds = 5.toUShort())
        val params = ConnectionParameters("test.mosquitto.org", 1883, false, connectionRequest)
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
        val params = ConnectionParameters("test.mosquitto.org", 1883, false, connectionRequest)
        val connection = PlatformSocketConnection(params)
        val result = connection.openConnectionAsync(true)
        block {
            result.await()
        }
    }

    @Test
    fun publishSingleMessageQos0() {
        val connectionRequest = ConnectionRequest(clientId = getClientId(), keepAliveSeconds = 5.toUShort())
        val params = ConnectionParameters("test.mosquitto.org", 1883, false, connectionRequest)
        val connection = PlatformSocketConnection(params)
        val result = connection.openConnectionAsync(true)
        block {
            result.await()
            val publishMessage = PublishMessage("yolo", "asdf".toByteArray())
            connection.clientToServer.send(publishMessage)
            assertEquals(connection.state.value, Open)
        }
    }

    @Test
    fun subscribeAndReceiveSuback() {
        val connectionRequest = ConnectionRequest(clientId = getClientId(), keepAliveSeconds = 5.toUShort())
        val params = ConnectionParameters("test.mosquitto.org", 1883, false, connectionRequest)
        val connection = PlatformSocketConnection(params)
        val result = connection.openConnectionAsync(true)
        var recvMessage = false
        block {
            result.await()
            val mutex = Mutex(true)
            launch(Dispatchers.Unconfined) {
                val controlPacket = connection.serverToClient.receive()
                assertTrue(controlPacket is SubscribeAcknowledgement)
                recvMessage = true
                mutex.unlock()
            }
            val subscribeRequest = SubscribeRequest(1.toUShort(), listOf(Subscription(MqttUtf8String("yolo"), AT_MOST_ONCE)))
            connection.clientToServer.send(subscribeRequest)
            mutex.withLock {} // lock until we get a message
            assertTrue(recvMessage)
        }
    }
}
