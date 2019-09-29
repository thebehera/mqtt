@file:Suppress("EXPERIMENTAL_API_USAGE")

package mqtt.client

import kotlinx.coroutines.Deferred
import kotlinx.coroutines.sync.Mutex
import mqtt.client.connection.parameters.ConnectionParameters
import mqtt.client.connection.parameters.RemoteHost
import mqtt.client.transport.OnMessageReceivedCallback
import mqtt.wire.control.packet.*
import mqtt.wire.data.QualityOfService
import mqtt.wire.data.QualityOfService.*
import mqtt.wire.data.topic.Filter
import mqtt.wire.data.topic.Name
import mqtt.wire.data.topic.SubscriptionCallback
import mqtt.wire4.control.packet.ConnectionRequest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.fail

val domain = "localhost"
val port = 60000

class ClientTests {

    val areWebsocketTestsEnabled = false

    @Test
    fun reconnectsAfterSocketConnectionFailure() {
        val request = ConnectionRequest(getClientId())
        val invalidBadPort = 1
        val client = MqttClient(
            ConnectionParameters(
                RemoteHost(
                    "localhost",
                    port = invalidBadPort,
                    request = request,
                    security = RemoteHost.Security(
                        isTransportLayerSecurityEnabled = false
                    ),
                    maxNumberOfRetries = 3
                )
            )
        )
        val job = client.startAsync()
        blockWithTimeout(5000) {
            job.await()
        }
        assertEquals(3, client.connectionCount)
    }

    fun createClient(websockets: Boolean = false, clientId: String = getClientId()): Pair<MqttClient, Deferred<Any>> {
        val request = ConnectionRequest(clientId, keepAliveSeconds = 10.toUShort())
        val client = MqttClient(
            ConnectionParameters(
                RemoteHost(
                    "localhost",
                    port = port,
                    request = request,
                    security = RemoteHost.Security(
                        isTransportLayerSecurityEnabled = false
                    ),
                    maxNumberOfRetries = 3
                )
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

    inline fun <reified T> blockUntilMessageReceived(
        topic: String, qos: QualityOfService,
        publishMessageNumber: UShort = getAndIncrementPacketIdentifier().toUShort(),
        cb: OnMessageReceivedCallback? = null
    ) {
        val (client, job) = createClient()
        println("created client")
        blockWithTimeout {
            println("client connected")
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
            println("publish")
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
                    assertEquals(publishMessageNumber, controlPacket.packetIdentifier.toUShort())
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
            val pubCompMutex = Mutex(true)
            client.session.everyRecvMessageCallback = object : OnMessageReceivedCallback {
                override fun onMessage(controlPacket: ControlPacket) {
                    if (controlPacket is ISubscribeAcknowledgement) {
                        pubCompMutex.unlock()
                    }
                }
            }
            println("sending subscribe")
            client.session.subscribe(Filter("hello"), AT_LEAST_ONCE, object : SubscriptionCallback<String> {
                override fun onMessageReceived(topic: Name, qos: QualityOfService, message: String?) {
                    println(message)
                }
            })
            println("subscribe sent")
            pubCompMutex.lock()
        }
    }

    @Test
    fun subscribeOnePublishAnotherWorks() {
        val ogMessage = "Hello2"
        blockWithTimeout {
            val client1Session1 = createClient().first
            val client2 = createClient().first
            val mutex = Mutex(true)
            client1Session1.subscribe<String>("yolo2/+", AT_MOST_ONCE) { topic, qos, message ->
                assertEquals(ogMessage, message)
                if (!mutex.isLocked) {
                    return@subscribe
                }
                mutex.unlock()
            }
            client2.session.publish("yolo2/23", AT_LEAST_ONCE, ogMessage)
            if (!mutex.isLocked) {
                mutex.lock()
            }
            println("continue")
        }
    }

    @Test
    fun subscribeOnePublishAnotherWorksWebSocketPublisher() {
        if (!areWebsocketTestsEnabled) {
            println("Warning:: Websocket tests are disabled")
            return
        }
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
        if (!areWebsocketTestsEnabled) {
            println("Warning:: Websocket tests are disabled")
            return
        }
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
