@file:Suppress("EXPERIMENTAL_API_USAGE")

package mqtt.client

import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.delay
import kotlinx.coroutines.withTimeout
import mqtt.wire.data.MqttUtf8String
import mqtt.wire.data.QualityOfService
import mqtt.wire4.control.packet.ConnectionRequest
import mqtt.wire4.control.packet.DisconnectNotification
import mqtt.wire4.control.packet.PublishMessage
import kotlin.test.Test
import kotlin.test.assertTrue
import kotlin.test.fail


class ConnectionTests {

    private val expectedConnectionTime = 5000L

    @Test
    fun connectDisconnectAndPublishMosquittoTestServerNotSecure() {
        val header = ConnectionRequest.VariableHeader(keepAliveSeconds = 5.toUShort())
        val payload = ConnectionRequest.Payload(clientId = MqttUtf8String("JavaSample"))
        val params = ConnectionParameters("test.mosquitto.org", 1883, false,
                ConnectionRequest(header, payload), reconnectIfNetworkLost = false)
        val connection = openConnection(params)
        block {
            val fixed = PublishMessage.FixedHeader(qos = QualityOfService.AT_LEAST_ONCE)
            val variable = PublishMessage.VariableHeader(MqttUtf8String("yolo"), 1.toUShort())
            params.clientToBroker.send(PublishMessage(fixed, variable))
            params.clientToBroker.send(DisconnectNotification)
            connection.await()
        }
    }

    @Test
    fun testConnectionTimeout() {
        val header = ConnectionRequest.VariableHeader(keepAliveSeconds = 5.toUShort())
        val payload = ConnectionRequest.Payload(clientId = MqttUtf8String("JavaSample"))
        val params = ConnectionParameters("test.mosquitto.org", 1883, false,
                ConnectionRequest(header, payload), reconnectIfNetworkLost = false, connectionTimeoutMilliseconds = 2)
        val connection = openConnection(params)
        block {
            withTimeout(100) {
                connection.await()
                assertTrue { connection.getCompleted() }
            }
        }
    }

    @Test
    fun testConnectionTimeoutReconnectingLogic() {
        val header = ConnectionRequest.VariableHeader(keepAliveSeconds = 5.toUShort())
        val payload = ConnectionRequest.Payload(clientId = MqttUtf8String("JavaSample"))
        var timeout = 2L
        block {
            while (timeout < expectedConnectionTime) {
                timeout *= 10
                var error = false
                try {
                    val params = ConnectionParameters("test.mosquitto.org", 1883, false,
                            ConnectionRequest(header, payload),
                            reconnectIfNetworkLost = true,
                            maxNumberOfRetries = 1,
                            connectionTimeoutMilliseconds = timeout)
                    println("connecting with timeout ms $timeout")
                    val connection = openConnection(params)
                    delay(1000)
                    params.clientToBroker.offer(DisconnectNotification)
                    connection.await()
                } catch (e: ConnectionTimeout) {
                    error = true
                } catch (e: TimeoutCancellationException) {
                    error = true
                } finally {
                    if (timeout > expectedConnectionTime) {
                        if (error) {
                            fail()
                        }
                    }
                }
            }

        }
    }

}
