package mqtt.client

import kotlinx.coroutines.Deferred
import kotlinx.coroutines.sync.Mutex
import mqtt.connection.ConnectionState
import mqtt.wire.data.QualityOfService
import mqtt.wire4.control.packet.ConnectionRequest
import kotlin.test.Test
import kotlin.test.assertEquals

class ClientJSTests {


    fun createClient(
        websockets: Boolean = true,
        clientId: String = getClientId()
    ): Pair<MqttClient, Deferred<ConnectionState>> {
        val request = ConnectionRequest(clientId, keepAliveSeconds = 10.toUShort())
        val client = MqttClient(
            RemoteHost(
                "localhost",
                port = port,
                request = request,
                security = RemoteHost.Security(
                    isTransportLayerSecurityEnabled = false
                ),
                websocket = RemoteHost.Websocket(websockets),
                maxNumberOfRetries = 3
            )
        )
        val job = client.connectAsync()
        return Pair(client, job)
    }

    suspend fun createClientAwaitConnection(websockets: Boolean = false, clientId: String = getClientId()): MqttClient {
        val (client, job) = createClient(websockets, clientId)
        job.await()
        return client
    }

    @Test
    fun subscribeOnePublishAnotherWorksWebSocketSubscriber() {
        val ogMessage = "Hello2"
        blockWithTimeout {
            val client1Session1 = createClientAwaitConnection(true)
            val client2 = createClientAwaitConnection()
            val mutex = Mutex(true)
            client1Session1.subscribe<String>("yolo2/+", QualityOfService.AT_MOST_ONCE, 4.toUShort()) { _, _, message ->
                assertEquals(ogMessage, message)
                mutex.unlock()
            }
            client2.session.publish("yolo2/23", QualityOfService.AT_LEAST_ONCE, 4.toUShort(), ogMessage)
            mutex.lock()
        }
    }
}