package mqtt.client.service

import android.annotation.SuppressLint
import android.app.Application
import androidx.lifecycle.AndroidViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import mqtt.client.persistence.MqttConnectionsDatabaseDescriptor
import mqtt.client.persistence.MqttSubscription
import mqtt.client.service.ipc.ClientToServiceConnection
import mqtt.client.session.SubscriptionManager
import mqtt.connection.IRemoteHost
import mqtt.connection.Open
import mqtt.wire.control.packet.ControlPacket
import mqtt.wire.control.packet.IPublishMessage
import mqtt.wire.data.QualityOfService
import mqtt.wire.data.topic.Node
import mqtt.wire.data.topic.SubscriptionCallback

abstract class AbstractMqttServiceViewModel(app: Application, val dbDescriptor: MqttConnectionsDatabaseDescriptor) :
    AndroidViewModel(app), CoroutineScope {
    val job = Job()
    val db by lazy { dbDescriptor.getDb(app) }
    override val coroutineContext = Dispatchers.Main + job
    val serviceConnection by lazy {
        ClientToServiceConnection(
            app,
            ConnectionManagerService::class.java,
            dbDescriptor
        )
    }
    @SuppressLint("UseSparseArrays")
    val subscriptions = HashMap<Int, SubscriptionManager>()

    /**
     * Create new managed mqtt connection
     */
    suspend fun createConnection(config: IRemoteHost, awaitOnConnectionState: Int? = Open.state) =
        serviceConnection.createNewConnection(config, awaitOnConnectionState)

    suspend inline fun <reified T : Any> subscribe(
        topicFilter: String,
        qualityOfService: QualityOfService,
        connectionId: Int,
        cb: SubscriptionCallback<T>
    ) {
        getSubscriptionManager(connectionId).register(Node.parse(topicFilter), cb)
        val subscription = MqttSubscription(connectionId, topicFilter, T::class.java.canonicalName!!)
        val result =
            db.mqttQueueDao().subscribe(MqttSubscription::class.java.canonicalName!!, qualityOfService, subscription)
        serviceConnection.notifySubscribe(
            result.queuedRowId,
            subscription.copy(packetIdentifier = result.packetIdentifier)
        )
    }

    suspend fun unsubscribe(topicFilter: String, connectionId: Int) {
        getSubscriptionManager(connectionId).unregister(Node.parse(topicFilter))
        db.mqttQueueDao().unsubscribe(connectionId, topicFilter)
        serviceConnection.notifyUnsubscribe(topicFilter, connectionId)
    }

    fun getSubscriptionManager(connectionId: Int): SubscriptionManager {
        synchronized(subscriptions) {
            val subscriptionManager = subscriptions[connectionId] ?: SubscriptionManager()
            subscriptions[connectionId] = subscriptionManager
            return subscriptionManager
        }
    }

    fun incomingMessageCallback(cb: (ControlPacket, Int) -> Unit) {
        serviceConnection.newConnectionManager.incomingMessageCallback = object : (ControlPacket, Int) -> Unit {
            override fun invoke(incoming: ControlPacket, connectionIdentifier: Int) {
                if (incoming is IPublishMessage) {
                    subscriptions[connectionIdentifier]?.handleIncomingPublish(incoming)
                }
                if (incoming is IPublishMessage && onIncomingPublish(incoming, connectionIdentifier)) {
                } else {
                    cb(incoming, connectionIdentifier)
                }
            }
        }
    }

    fun onIncomingPublish(incoming: IPublishMessage, connectionId: Int): Boolean {
        getSubscriptionManager(connectionId).handleIncomingPublish(incoming)
        return true
    }

    fun messageSentCallback(cb: (ControlPacket, Int) -> Unit) {
        serviceConnection.newConnectionManager.outgoingMessageCallback = cb
    }

    suspend fun notifyPublish(notifyPublish: ClientToServiceConnection.NotifyPublish) {
        serviceConnection.notifyPublish(notifyPublish)
    }

    override fun onCleared() {
        serviceConnection.unbind(getApplication())
        super.onCleared()
    }
}
