package mqtt.client

import kotlinx.coroutines.Deferred
import kotlinx.coroutines.sync.Mutex
import mqtt.client.connection.ConnectionParameters
import mqtt.wire.data.QualityOfService
import mqtt.wire4.control.packet.ConnectionRequest
import kotlin.test.Test
import kotlin.test.assertEquals

class ClientJSTests {


    fun createClient(websockets: Boolean = false, clientId: String = getClientId()): Pair<MqttClient, Deferred<Unit>> {
        val request = ConnectionRequest(clientId, keepAliveSeconds = 10.toUShort())
        val port = if (websockets) {
            60002
        } else {
            60000
        }
        val params = ConnectionParameters(
            "172.16.74.128", port, secure = false,
            connectionRequest = request, useWebsockets = websockets,
            logIncomingControlPackets = true,
            logOutgoingControlPackets = true,
            logConnectionAttempt = true,
            logIncomingPublish = true,
            logOutgoingPublishOrSubscribe = true,
            connectionTimeoutMilliseconds = 15000
        )
        val client = MqttClient(params)
        val job = client.startAsyncWaitUntilFirstConnection()
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
            client1Session1.subscribe<String>("yolo2/+", QualityOfService.AT_MOST_ONCE) { topic, qos, message ->
                assertEquals(ogMessage, message)
                mutex.unlock()
            }
            client2.session.publish("yolo2/23", QualityOfService.AT_LEAST_ONCE, ogMessage)
            mutex.lock()
        }
    }
}