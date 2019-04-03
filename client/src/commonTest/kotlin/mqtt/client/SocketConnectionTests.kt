@file:Suppress("EXPERIMENTAL_API_USAGE")

package mqtt.client

import kotlinx.coroutines.withTimeout
import mqtt.wire.data.MqttUtf8String
import mqtt.wire4.control.packet.ConnectionRequest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class SocketConnectionTests {

    @Test
    fun connectDisconnect() {
        val header = ConnectionRequest.VariableHeader(keepAliveSeconds = 5.toUShort())
        val payload = ConnectionRequest.Payload(clientId = MqttUtf8String("JavaSample"))
        val params = ConnectionParameters("test.mosquitto.org", 1883, false,
                ConnectionRequest(header, payload), reconnectIfNetworkLost = false)
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
        val header = ConnectionRequest.VariableHeader(keepAliveSeconds = 5.toUShort())
        val payload = ConnectionRequest.Payload(clientId = MqttUtf8String("JavaSample"))
        val params = ConnectionParameters("test.mosquitto.org", 1883, false,
                ConnectionRequest(header, payload))
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
        val header = ConnectionRequest.VariableHeader(keepAliveSeconds = 5.toUShort())
        val payload = ConnectionRequest.Payload(clientId = MqttUtf8String("JavaSample"))
        val params = ConnectionParameters("test.mosquitto.org", 1883, false,
                ConnectionRequest(header, payload))
        val connection = PlatformSocketConnection(params)
        val result = connection.openConnectionAsync(true)
        block {
            result.await()
        }
    }
}
