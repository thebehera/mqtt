@file:Suppress("EXPERIMENTAL_API_USAGE")

package mqtt.client

import kotlinx.coroutines.Deferred
import kotlinx.coroutines.sync.Mutex
import mqtt.client.connection.ConnectionParameters
import mqtt.client.transport.OnMessageReceivedCallback
import mqtt.wire.control.packet.*
import mqtt.wire.data.QualityOfService
import mqtt.wire.data.QualityOfService.*
import mqtt.wire.data.topic.Filter
import mqtt.wire.data.topic.Name
import mqtt.wire.data.topic.SubscriptionCallback
import mqtt.wire4.control.packet.ConnectionRequest
import platform.Platform
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.fail

class ClientTests {

    @Test
    fun reconnectsAfterSocketConnectionFailure() {
        val request = ConnectionRequest(getClientId())
        val invalidBadPort = 1
        val params = ConnectionParameters("localhost", invalidBadPort, false, request,
                maxNumberOfRetries = 3)
        val client = MqttClient(params)
        val job = client.startAsync()
        blockWithTimeout(5000) {
            job.await()
        }
        assertEquals(3, client.connectionCount)
    }

    fun createClient(websockets: Boolean = false, clientId: String = getClientId()): Pair<MqttClient, Deferred<Unit>> {
        val request = ConnectionRequest(clientId, keepAliveSeconds = 10.toUShort())
        val port = if (websockets) {
            60002
        } else {
            60000
        }
        var ws = websockets
        if (Platform.name == "JS") {
            ws = true
        }
        val params = ConnectionParameters(
            "172.16.74.128", port, secure = false,
            connectionRequest = request, useWebsockets = ws,
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

    inline fun <reified T> blockUntilMessageReceived(
        topic: String, qos: QualityOfService,
        publishMessageNumber: UShort = getAndIncrementPacketIdentifier(),
        cb: OnMessageReceivedCallback? = null
    ) {
        val (client, job) = createClient()
        blockWithTimeout {
            job.await()
            val pubCompMutex = Mutex(true)
            client.session.everyRecvMessageCallback = object : OnMessageReceivedCallback {
                override fun onMessage(controlPacket: ControlPacket) {
                    if (controlPacket is T) {
                        cb?.onMessage(controlPacket)
                        pubCompMutex.unlock()
                    }
                }
            }
            client.session.publish(topic, qos, publishMessageNumber)
            pubCompMutex.lock()
        }
    }

    @Test
    fun publishQos2PublishCompleteReceived() {
        val publishMessageNumber = UShort.MAX_VALUE
        blockUntilMessageReceived<IPublishComplete>(
            "yolo",
            EXACTLY_ONCE,
            publishMessageNumber,
            cb = object : OnMessageReceivedCallback {
                override fun onMessage(controlPacket: ControlPacket) {
                    if (controlPacket !is IPublishComplete) {
                        fail("invalid control packet type")
                    }
                    assertEquals(publishMessageNumber, controlPacket.packetIdentifier)
                }
            })
    }

    @Test
    fun publishQos1PublishAckReceived() {
        blockUntilMessageReceived<IPublishAcknowledgment>("yolo2", AT_LEAST_ONCE,
            cb = object : OnMessageReceivedCallback {
                override fun onMessage(controlPacket: ControlPacket) {
                    if (controlPacket !is IPublishAcknowledgment) {
                        fail("invalid control packet type")
                    }
                }
            })
    }


    @Test
    fun subscribeAckReceived() {
        val (client, job) = createClient()
        blockWithTimeout {
            job.await()
            val pubCompMutex = Mutex(true)
            client.session.everyRecvMessageCallback = object : OnMessageReceivedCallback {
                override fun onMessage(controlPacket: ControlPacket) {
                    if (controlPacket is ISubscribeAcknowledgement) {
                        pubCompMutex.unlock()
                    }
                }
            }
            client.session.subscribe(Filter("hello"), AT_LEAST_ONCE, object : SubscriptionCallback<String> {
                override fun onMessageReceived(topic: Name, qos: QualityOfService, message: String?) {
                    println(message)
                }
            })
            pubCompMutex.lock()
        }
    }

    @Test
    fun subscribeOnePublishAnotherWorks() {
        val ogMessage = "Hello2"
        blockWithTimeout {
            val client1Session1 = createClientAwaitConnection()
            val client2 = createClientAwaitConnection()
            val mutex = Mutex(true)
            client1Session1.subscribe<String>("yolo2/+", AT_MOST_ONCE) { topic, qos, message ->
                assertEquals(ogMessage, message)
                mutex.unlock()
            }
            client2.session.publish("yolo2/23", AT_LEAST_ONCE, ogMessage)
            mutex.lock()
        }
    }

    @Test
    fun subscribeOnePublishAnotherWorksWebSocketPublisher() {
        val ogMessage = "Hello2"
        blockWithTimeout {
            val client1Session1 = createClientAwaitConnection()
            val client2 = createClientAwaitConnection(true)
            val mutex = Mutex(true)
            client1Session1.subscribe<String>("yolo2/+", AT_MOST_ONCE) { topic, qos, message ->
                assertEquals(ogMessage, message)
                mutex.unlock()
            }
            client2.session.publish("yolo2/23", AT_LEAST_ONCE, ogMessage)
            mutex.lock()
        }
    }

    @Test
    fun subscribeOnePublishAnotherWorksWebSocketSubscriber() {
        val ogMessage = "Hello2"
        val mutex = Mutex(true)
        blockWithTimeout {
            val client1Session1 = createClientAwaitConnection(true)
            val client2 = createClientAwaitConnection()
            client1Session1.subscribe<String>("yolo2/+", AT_MOST_ONCE) { topic, qos, message ->
                assertEquals(ogMessage, message)
                if (mutex.isLocked) {
                    mutex.unlock()
                }
            }
            client2.session.publish("yolo2/23", AT_LEAST_ONCE, ogMessage)
            mutex.lock()
        }
    }

}