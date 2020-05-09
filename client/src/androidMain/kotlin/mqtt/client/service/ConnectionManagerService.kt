@file:Suppress("EXPERIMENTAL_API_USAGE")

package mqtt.client.service

import android.annotation.SuppressLint
import android.content.Intent
import android.os.*
import android.util.Log
import kotlinx.coroutines.launch
import mqtt.client.persistence.MqttConnectionsDatabaseDescriptor
import mqtt.client.persistence.MqttSubscription
import mqtt.client.persistence.PersistableRemoteHostV4
import mqtt.client.service.ipc.BoundClientToService
import mqtt.client.service.ipc.ClientToServiceConnection.NotifyPublish
import mqtt.client.service.ipc.ServiceToBoundClient
import mqtt.client.service.ipc.ServiceToBoundClient.CONNECTION_STATE_CHANGED
import mqtt.connection.MqttConnectionStateUpdated
import mqtt.connection.Open
import mqtt.wire.control.packet.IConnectionAcknowledgment
import mqtt.wire.control.packet.ISubscribeAcknowledgement
import kotlin.time.ExperimentalTime

private const val TAG = "[MQTT][SiCo]"
const val MESSAGE_PAYLOAD = "msg_payload"
const val REGISTER_CLIENT = Int.MIN_VALUE
const val UNREGISTER_CLIENT = Int.MIN_VALUE + 1


@ExperimentalTime
class ConnectionManagerService : CoroutineService() {

    private lateinit var dbProvider: MqttConnectionsDatabaseDescriptor

    @SuppressLint("UseSparseArrays")
    private val connectionManagers = HashMap<Int, ConnectionManager>()

    private val boundClients by lazy {
        BoundClientsObserver(newClientCb) { messageFromBoundClient ->
            messageFromBoundClient.data?.classLoader = classLoader
            handleMessage(messageFromBoundClient)
        }
    }
    private val newClientCb: (Messenger) -> Unit = { messenger ->
        connectionManagers.values.forEach { connectionManager ->
            val msg = buildConnectionChangeToClients(
                MqttConnectionStateUpdated(
                    connectionManager.remoteHost.connectionIdentifier(), connectionManager.connectionState
                )
            )
            messenger.send(msg)
        }
    }

    override fun onBind(intent: Intent): IBinder {
        setupDbAndInitializeConnections(intent)
        return boundClients.binder
    }

    private fun handleMessage(msg: Message) {
        when {
            msg.what == BoundClientToService.QUEUE_INSERTED.position -> {
                val bundle: Bundle = msg.data ?: return
                val publish = NotifyPublish::class.java.canonicalName ?: return
                val notifyPublishFromClient = bundle.getParcelable<NotifyPublish>(publish)!!
                val connection = connectionManagers[notifyPublishFromClient.connectionIdentifier] ?: return
                launch {
                    val persistence = dbProvider.getPersistence(
                        this@ConnectionManagerService,
                        coroutineContext,
                        notifyPublishFromClient.connectionIdentifier
                    )
                    val packet = persistence.get(notifyPublishFromClient.messageId) ?: return@launch
                    connection.client.session.send(packet)
                }
            }
            msg.what == BoundClientToService.SUBSCRIBE.position -> {
                val bundle: Bundle = msg.data ?: return
                val subscriptionClass = MqttSubscription::class.java.canonicalName!!
                val subscription = bundle.getParcelable<MqttSubscription>(subscriptionClass) ?: return
                val rowId = bundle.getLong("rowId")
                val connection = connectionManagers[subscription.connectionIdentifier] ?: return
                launch {
                    val db = dbProvider.getDb(this@ConnectionManagerService).mqttQueueDao()
                    val queuedMqtt = db.getQueuedObjectByRowId(rowId) ?: return@launch
                    val klass = Class.forName(subscription.kclass).kotlin

                    dbProvider.subscribe(
                        connection.client, subscription.packetIdentifier.toUShort(),
                        subscription.topicFilter, queuedMqtt.qos, klass
                    ) { filter, qos, msg ->
                        println("Incoming subscribe msg $filter $qos $msg")
                        if (msg !is Parcelable) return@subscribe

                    }

                }
            }
            else -> when (val data = msg.data?.getParcelable<Parcelable>(MESSAGE_PAYLOAD) ?: return) {
                is PersistableRemoteHostV4 -> launch {
                    Log.i("RAHUL", "Handle msg")
                    connect(data)
                }
            }
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        setupDbAndInitializeConnections(intent)
        return super.onStartCommand(intent, flags, startId)
    }

    private suspend fun connect(connectionParameters: PersistableRemoteHostV4) {
        Log.i("RAHUL", "Connect")
        if (connectionManagers[connectionParameters.connectionIdentifier()] != null) {
            return
        }
        Log.i("RAHUL", "Connect go")
        val persistence = dbProvider.getPersistence(this, coroutineContext, connectionParameters.connectionIdentifier())
        val connectionChangeCallback: ((MqttConnectionStateUpdated) -> Unit) = {
            Log.i("RAHUL", "Connection change to ${it.state}")
            boundClients.sendMessageToClients(buildConnectionChangeToClients(it))
        }
        val connectionManager = ConnectionManager(connectionParameters, persistence) { controlPacket, remoteHostId ->
            if (controlPacket is IConnectionAcknowledgment) {
                connectionChangeCallback(MqttConnectionStateUpdated(connectionParameters, Open(controlPacket)))
            } else if (controlPacket is ISubscribeAcknowledgement) {
                connectionManagers[remoteHostId]?.client?.session?.state?.subscriptionAcknowledgementReceived(controlPacket)
            } else {
                val msg = Message.obtain()
                msg.what = ServiceToBoundClient.INCOMING_CONTROL_PACKET.position
                msg.arg1 = remoteHostId
                val bundle = Bundle()
                bundle.putParcelable(MESSAGE_PAYLOAD, controlPacket)
                msg.data = bundle
                boundClients.sendMessageToClients(msg)
            }
        }
        Log.i("RAHUL", "Set connection: ${connectionParameters.connectionIdentifier()}")
        connectionManagers[connectionParameters.connectionIdentifier()] = connectionManager
        dbProvider.getDb(this).remoteHostsDao().addOrUpdate(connectionParameters)
        connectionManager.client.session.outboundCallback = { controlPacketSentToServer, remoteHostId ->

            Log.i("RAHUL", "Send to server $controlPacketSentToServer")
            val msg = Message.obtain()
            msg.what = ServiceToBoundClient.OUTGOING_CONTROL_PACKET.position
            msg.arg1 = remoteHostId
            val bundle = Bundle()
            bundle.putParcelable(MESSAGE_PAYLOAD, controlPacketSentToServer)
            msg.data = bundle
            boundClients.sendMessageToClients(msg)
        }
        connectionManager.connect(connectionChangeCallback)
    }

    fun buildConnectionChangeToClients(connectionStateUpdated: MqttConnectionStateUpdated): Message {
        val msg = Message.obtain(null, CONNECTION_STATE_CHANGED.position)
        val bundle = Bundle()
        bundle.putParcelable(MESSAGE_PAYLOAD, connectionStateUpdated)
        msg.data = bundle
        return msg
    }

    private fun setupDbAndInitializeConnections(intent: Intent?) {
        if (setupDatabase(intent)) {
            val db = dbProvider.getDb(this)
            launch {
                db.remoteHostsDao().getAllConnections().forEach {
                    Log.i("RAHUL", "setup")
                    connect(it)
                }
            }
        }
    }

    private fun setupDatabase(intent: Intent?): Boolean {
        synchronized(this) {
            intent ?: return false
            if (::dbProvider.isInitialized) {
                return false
            }
            intent.setExtrasClassLoader(classLoader)
            dbProvider = intent.getParcelableExtra(MqttConnectionsDatabaseDescriptor.TAG) ?: return false
            return true
        }
    }

    private suspend fun disconnect() {
        connectionManagers.values.forEach { it.disconnectAsync().await() }
        stopSelf()
    }

    override fun onDestroy() {
        launch { disconnect() }
        super.onDestroy()
    }

}
