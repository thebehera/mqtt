@file:Suppress("EXPERIMENTAL_API_USAGE")

package mqtt.client

import kotlinx.coroutines.*
import kotlinx.coroutines.sync.Mutex
import mqtt.client.persistence.MemoryQueuedObjectCollection
import mqtt.client.persistence.QueuedObjectCollection
import mqtt.client.platform.PlatformCoroutineDispatcher
import mqtt.client.session.ClientSession
import mqtt.client.session.ClientSessionState
import mqtt.client.transport.OnMessageReceivedCallback
import mqtt.connection.ConnectionFailure
import mqtt.connection.ConnectionState
import mqtt.connection.IRemoteHost
import mqtt.connection.Open
import mqtt.wire.data.QualityOfService
import mqtt.wire.data.topic.Filter
import mqtt.wire.data.topic.Name
import mqtt.wire.data.topic.SubscriptionCallback
import kotlin.coroutines.CoroutineContext
import kotlin.reflect.KClass

data class MqttClient(
    val remoteHost: IRemoteHost,
    val otherMsgCallback: OnMessageReceivedCallback? = null,
    val queuedObjectCollection: QueuedObjectCollection = MemoryQueuedObjectCollection(remoteHost.connectionIdentifier())
) : CoroutineScope {
    private val job: Job = Job()
    private val dispatcher = PlatformCoroutineDispatcher.dispatcher
    val state by lazy {
        ClientSessionState(queuedObjectCollection, remoteHost).also {
            launch {
                it.start()
            }
        }
    }
    override val coroutineContext: CoroutineContext = job + dispatcher
    var connectionCount = 0
    val session by lazy {
        ClientSession(remoteHost, Job(job), state).also {
            it.everyRecvMessageCallback = otherMsgCallback
        }
    }

    fun connectAsync() = async {
        val lock = Mutex(true)
        lateinit var queuedConnectionResult: ConnectionState
        val result = startAsync {
            queuedConnectionResult = it
            lock.unlock()
        }
        lock.lock()
        return@async queuedConnectionResult
    }

    fun startAsync(newConnectionCb: ((ConnectionState) -> Unit)? = null) = async {
        if (session.transport?.isOpenAndActive() == true) {
            return@async true
        }
        return@async retryIO(remoteHost.maxNumberOfRetries) {
            val result = try {
                if (isActive) {
                    val result = session.connect()
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
                false
            }
            result
        }
    }

    suspend fun <T : Any> subscribe(
        topicFilter: String, qos: QualityOfService, packetIdentifier: UShort, typeClass: KClass<T>,
        callback: (topic: Name, qos: QualityOfService, message: T?) -> Unit
    ) {
        val subscriptionCallback = object : SubscriptionCallback<T> {
            override fun onMessageReceived(topic: Name, qos: QualityOfService, message: T?) = callback(topic, qos, message)
        }
        session.subscribe(packetIdentifier, Filter(topicFilter), qos, typeClass, subscriptionCallback)
    }

    suspend inline fun <reified T : Any> subscribe(
        topicFilter: String, qos: QualityOfService, packetIdentifier: UShort,
        noinline callback: (topic: Name, qos: QualityOfService, message: T?) -> Unit
    ) = subscribe(topicFilter, qos, packetIdentifier, T::class, callback)

    suspend fun <T : Any> publish(
        topic: String,
        qos: QualityOfService,
        packetIdentifier: UShort,
        typeClass: KClass<T>,
        message: T
    ) {
        session.publish(topic, qos, packetIdentifier, typeClass, message)
    }

    suspend inline fun <reified T : Any>
            publish(topic: String, qos: QualityOfService, packetIdentifier: UShort, message: T) =
        publish(topic, qos, packetIdentifier, T::class, message)

    fun disconnectAsync() = async {
        val disconnect = session.disconnectAsync()
        disconnect
    }
}
