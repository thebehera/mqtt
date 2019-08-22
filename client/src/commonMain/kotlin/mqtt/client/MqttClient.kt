@file:Suppress("EXPERIMENTAL_API_USAGE")

package mqtt.client

import io.ktor.http.Url
import kotlinx.coroutines.*
import kotlinx.coroutines.sync.Mutex
import mqtt.client.connection.Open
import mqtt.client.platform.PlatformCoroutineDispatcher
import mqtt.client.session.ClientSession
import mqtt.client.session.ClientSessionState
import mqtt.connection.IMqttConfiguration
import mqtt.wire.data.MqttUtf8String
import mqtt.wire.data.QualityOfService
import mqtt.wire.data.topic.Filter
import mqtt.wire.data.topic.Name
import mqtt.wire.data.topic.SubscriptionCallback
import kotlin.coroutines.CoroutineContext
import kotlin.reflect.KClass

data class MqttClient(override val config: IMqttConfiguration) : SimpleMqttClient {
    private val job: Job = Job()
    private val dispatcher = PlatformCoroutineDispatcher.dispatcher
    val state by lazy {
        ClientSessionState().also {
            launch {
                val host = config.remoteHost
                it.start(MqttUtf8String(host.request.clientIdentifier), Url(host.name))
            }
        }
    }
    override val coroutineContext: CoroutineContext = job + dispatcher
    var connectionCount = 0
    val session by lazy { ClientSession(config, Job(job), state) }
    val log by lazy { config.logConfiguration.getLogClass().connection }


    override fun connectAsync() = async {
        val lock = Mutex(true)
        try {
            log?.verbose("connectAsync - startAsync")
            val result = startAsync(Runnable {
                log?.verbose("unlock")
                lock.unlock()
                log?.verbose("unlocked")
            })
            log?.verbose("result startAsync = $result")
            return@async result
        } finally {
            log?.verbose("lock")
            lock.lock()
            log?.verbose("unlocked")
        }
    }

    fun startAsync(newConnectionCb: Runnable? = null) = async {
        if (session.transport?.isOpenAndActive() == true) {
            log?.verbose("transport is open and active")
            return@async true
        }

        log?.verbose("start async")
        return@async retryIO(config.remoteHost.maxNumberOfRetries) {
            val result = try {
                if (isActive) {
                    log?.verbose("connecting session")
                    val result = session.connect()
                    log?.verbose("connected session: $result")
                    connectionCount++
                    newConnectionCb?.run()
                    session.awaitSocketClose()
                    result is Open
                } else {
                    false
                }
            } catch (e: Exception) {
                log?.exceptionCausingReconnect(e)
                false
            }
            log?.verbose("done connecting $result")
            result
        }
    }

    override suspend fun <T : Any> subscribe(
        topicFilter: String, qos: QualityOfService, typeClass: KClass<T>,
        callback: (topic: Name, qos: QualityOfService, message: T?) -> Unit
    ) {
        log?.verbose("alloc susbscription")
        val subscriptionCallback = object : SubscriptionCallback<T> {
            override fun onMessageReceived(topic: Name, qos: QualityOfService, message: T?) = callback(topic, qos, message)
        }
        log?.verbose("subscribing to $topicFilter ($qos) with class $typeClass")
        session.subscribe(Filter(topicFilter), qos, typeClass, subscriptionCallback)
        log?.verbose("subscribed to $topicFilter ($qos) with class $typeClass")
    }

    suspend inline fun <reified T : Any> subscribe(
        topicFilter: String, qos: QualityOfService,
        noinline callback: (topic: Name, qos: QualityOfService, message: T?) -> Unit
    ) = subscribe(topicFilter, qos, T::class, callback)

    override suspend fun <T : Any> publish(topic: String, qos: QualityOfService, typeClass: KClass<T>, message: T) {
        log?.verbose("publish to $topic ($qos): $message")
        session.publish(topic, qos, typeClass, message)
        log?.verbose("published to $topic ($qos): $message")
    }

    suspend inline fun <reified T : Any> publish(topic: String, qos: QualityOfService, message: T) =
        publish(topic, qos, T::class, message)

    override fun disconnectAsync() = async {
        log?.verbose("disconnecting")
        val disconnect = session.disconnectAsync()
        log?.verbose("disconnected $disconnect")
        disconnect
    }
}
