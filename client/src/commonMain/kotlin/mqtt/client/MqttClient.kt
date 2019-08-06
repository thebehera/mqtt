@file:Suppress("EXPERIMENTAL_API_USAGE")

package mqtt.client

import io.ktor.http.Url
import kotlinx.coroutines.*
import kotlinx.coroutines.sync.Mutex
import mqtt.client.connection.ConnectionParameters
import mqtt.client.connection.Open
import mqtt.client.platform.PlatformCoroutineDispatcher
import mqtt.client.session.ClientSession
import mqtt.client.session.ClientSessionState
import mqtt.wire.data.MqttUtf8String
import mqtt.wire.data.QualityOfService
import mqtt.wire.data.topic.Filter
import mqtt.wire.data.topic.Name
import mqtt.wire.data.topic.SubscriptionCallback
import kotlin.coroutines.CoroutineContext

data class MqttClient(val params: ConnectionParameters) : CoroutineScope {
    private val job: Job = Job()
    private val dispatcher = PlatformCoroutineDispatcher.dispatcher
    val state by lazy {
        ClientSessionState().also {
            launch {
                it.start(MqttUtf8String(params.connectionRequest.clientIdentifier), Url(params.hostname))
            }
        }
    }
    override val coroutineContext: CoroutineContext = job + dispatcher
    var connectionCount = 0
    val session by lazy { ClientSession(params, Job(job), state) }

    fun connectAsync() = async {
        val lock = Mutex(true)
        try {
            return@async startAsync(Runnable {
                lock.unlock()
            }).await()
        } finally {
            lock.lock()
        }
    }

    internal fun startAsync(newConnectionCb: Runnable? = null) = async {
        if (session.transport?.isOpenAndActive() == true) {
            return@async true
        }

        println("start async")
        return@async retryIO(params.maxNumberOfRetries) {
            val result = try {
                if (isActive) {
                    println("connecting session")
                    val result = session.connect()
                    connectionCount++
                    newConnectionCb?.run()
                    session.awaitSocketClose()
                    result is Open
                } else {
                    false
                }
            } catch (e: Exception) {
                println(e)
                false
            }
            println("done connecting?")
            result
        }
    }

    suspend inline fun <reified T : Any> subscribe(topicFilter: String, qos: QualityOfService,
                                                   crossinline callback: (topic: Name, qos: QualityOfService, message: T?) -> Unit) {
        val subscriptionCallback = object : SubscriptionCallback<T> {
            override fun onMessageReceived(topic: Name, qos: QualityOfService, message: T?) = callback(topic, qos, message)
        }
        session.subscribe(Filter(topicFilter), qos, subscriptionCallback)
    }

    suspend inline fun <reified T : Any> publish(topic: String, qos: QualityOfService, message: T) = session.publishGeneric(topic, qos, message)

    fun disconnectAsync() = async {
        session.disconnectAsync()
    }
}