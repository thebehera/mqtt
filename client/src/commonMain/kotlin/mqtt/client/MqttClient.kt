@file:Suppress("EXPERIMENTAL_API_USAGE")

package mqtt.client

import io.ktor.http.Url
import kotlinx.coroutines.*
import kotlinx.coroutines.sync.Mutex
import mqtt.client.platform.PlatformCoroutineDispatcher
import mqtt.client.session.ClientSession
import mqtt.client.session.ClientSessionState
import mqtt.client.transport.OnMessageReceivedCallback
import mqtt.connection.ConnectionFailure
import mqtt.connection.ConnectionState
import mqtt.connection.IMqttConfiguration
import mqtt.connection.Open
import mqtt.wire.data.MqttUtf8String
import mqtt.wire.data.QualityOfService
import mqtt.wire.data.topic.Filter
import mqtt.wire.data.topic.Name
import mqtt.wire.data.topic.SubscriptionCallback
import kotlin.coroutines.CoroutineContext
import kotlin.reflect.KClass

data class MqttClient(
    override val config: IMqttConfiguration,
    val otherMsgCallback: OnMessageReceivedCallback? = null
) : SimpleMqttClient {
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
    val session by lazy { ClientSession(config, Job(job), state).also { it.callback = otherMsgCallback } }
    val log by lazy { config.logConfiguration.getLogClass().connection }

    override fun connectAsync() = async {
        val lock = Mutex(true)
        log?.verbose("connectAsync - startAsync")
        lateinit var queuedConnectionResult: ConnectionState
        val result = startAsync {
            queuedConnectionResult = it
            log?.verbose("unlock")
            lock.unlock()
            log?.verbose("unlocked")
        }
        log?.verbose("lock")
        lock.lock()
        log?.verbose("unlocked result startAsync = $result")
        return@async queuedConnectionResult
    }

    fun startAsync(newConnectionCb: ((ConnectionState) -> Unit)? = null) = async {
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
                    newConnectionCb?.invoke(result)
                    session.awaitSocketClose()
                    result is Open
                } else {
                    newConnectionCb?.invoke(ConnectionFailure(CancellationException("Client cancelled")))
                    false
                }
            } catch (e: Exception) {
                newConnectionCb?.invoke(ConnectionFailure(e))
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
