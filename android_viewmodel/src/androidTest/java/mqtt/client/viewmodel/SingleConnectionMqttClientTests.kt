package mqtt.client.viewmodel

import android.util.Log
import androidx.test.ext.junit.runners.AndroidJUnit4
import junit.framework.Assert.assertEquals
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.withTimeout
import mqtt.client.MqttClient
import mqtt.client.connection.ConnectionParameters
import mqtt.client.transport.OnMessageReceivedCallback
import mqtt.time.currentTimestampMs
import mqtt.wire.control.packet.ControlPacket
import mqtt.wire.control.packet.ISubscribeAcknowledgement
import mqtt.wire.data.QualityOfService
import mqtt.wire.data.topic.Filter
import mqtt.wire.data.topic.Name
import mqtt.wire.data.topic.SubscriptionCallback
import mqtt.wire4.control.packet.ConnectionRequest
import org.junit.Test
import org.junit.runner.RunWith

val domain = "192.168.1.98"
val port = 60000

@RunWith(AndroidJUnit4::class)
class SingleConnectionMqttClientTests {

    @Test
    fun subscribeAckReceived() {

        Log.i("RAHUL", "start")
        val (client, job) = createClient()
        blockWithTimeout {
            Log.i("RAHUL", "client created")
            job.await()
            Log.i("RAHUL", "connected")
            val pubCompMutex = Mutex(true)
            client.session.everyRecvMessageCallback = object : OnMessageReceivedCallback {
                override fun onMessage(controlPacket: ControlPacket) {
                    if (controlPacket is ISubscribeAcknowledgement) {
                        pubCompMutex.unlock()
                    }
                }
            }
            client.session.subscribe<String>(
                Filter("hello"),
                QualityOfService.AT_LEAST_ONCE, object : SubscriptionCallback<String> {
                    override fun onMessageReceived(topic: Name, qos: QualityOfService, message: String?) {
                        println(message)
                    }
                })
            pubCompMutex.lock()
        }
    }

    @Test
    fun reconnectsAfterSocketConnectionFailure() {
        val request = ConnectionRequest(getClientId())
        val invalidBadPort = 1
        val params = ConnectionParameters(
            "localhost", invalidBadPort, false, request,
            maxNumberOfRetries = 3
        )
        val client = MqttClient(params)
        val job = client.connectAsync()
        blockWithTimeout(5000) {
            job.await()
        }
        assertEquals(3, client.connectionCount)
        job.cancel()
    }


    fun <T> blockWithTimeout(timeoutMs: Long = 15000L, body: suspend CoroutineScope.() -> T) {
        runBlocking {
            withTimeout(timeoutMs) {
                body()
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

    fun createClient(websockets: Boolean = false, clientId: String = getClientId()): Pair<MqttClient, Deferred<Any>> {
        val request = ConnectionRequest(clientId, keepAliveSeconds = 10.toUShort())
        var ws = websockets
        val params = ConnectionParameters(
            domain, port, secure = false,
            connectionRequest = request, useWebsockets = ws,
            logIncomingControlPackets = true,
            logOutgoingControlPackets = true,
            logConnectionAttempt = true,
            logIncomingPublish = true,
            logOutgoingPublishOrSubscribe = true,
            connectionTimeoutMilliseconds = 15000
        )
        val client = MqttClient(params)
        val job = client.connectAsync()
        return Pair(client, job)
    }

}