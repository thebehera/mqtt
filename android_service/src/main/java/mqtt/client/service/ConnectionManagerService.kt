package mqtt.client.service

import android.content.Intent
import android.os.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import mqtt.client.connection.parameters.PersistableRemoteHostV4
import mqtt.client.connection.parameters.RemoteHostDao
import mqtt.client.service.ipc.*
import mqtt.client.service.ipc.ServiceToBoundClient.CONNECTION_STATE_CHANGED
import mqtt.connection.IRemoteHost
import mqtt.connection.MqttConnectionStateUpdated

private const val TAG = "[MQTT][SiCo]"
const val MESSAGE_PAYLOAD = "msg_payload"

class ConnectionManagerService : CoroutineService() {

    private lateinit var connectionsDao: RemoteHostDao
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
        if (msg.what == BoundClientToService.QUEUE_INSERTED.position) {
            val bundle: Bundle? = msg.data
            val rowId = bundle?.getLong(rowIdKey)
            val tableName = bundle?.getString(tableNameKey)
            tableName?.toString()
            return
        }
        when (val data = msg.data?.getParcelable<Parcelable>(MESSAGE_PAYLOAD) ?: return) {
            is IRemoteHost -> launch(Dispatchers.Default) {
                connect(data)
            }
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        setupDbAndInitializeConnections(intent)
        return super.onStartCommand(intent, flags, startId)
    }

    private suspend fun connect(connectionParameters: IRemoteHost) {
        if (connectionManagers[connectionParameters.connectionIdentifier()] != null) {
            return
        }
        val connectionManager = ConnectionManager(connectionParameters) { controlPacket, remoteHostId ->
            val msg = Message.obtain()
            msg.what = ServiceToBoundClient.INCOMING_CONTROL_PACKET.position
            msg.arg1 = remoteHostId
            val bundle = Bundle()
            bundle.putParcelable(MESSAGE_PAYLOAD, controlPacket)
            msg.data = bundle
            boundClients.sendMessageToClients(msg)
        }
        connectionManagers[connectionParameters.connectionIdentifier()] = connectionManager
        if (connectionParameters is PersistableRemoteHostV4) {
            connectionsDao.addOrUpdate(connectionParameters)
        }
        connectionManager.client.session.outboundCallback = { controlPacketSentToServer, remoteHostId ->
            val msg = Message.obtain()
            msg.what = ServiceToBoundClient.OUTGOING_CONTROL_PACKET.position
            msg.arg1 = remoteHostId
            val bundle = Bundle()
            bundle.putParcelable(MESSAGE_PAYLOAD, controlPacketSentToServer)
            msg.data = bundle
            boundClients.sendMessageToClients(msg)
        }
        connectionManager.connect {
            boundClients.sendMessageToClients(buildConnectionChangeToClients(it))
        }
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
            launch {
                connectionsDao.getAllConnections().forEach {
                    connect(it)
                }
            }
        }
    }

    private fun setupDatabase(intent: Intent?): Boolean {
        intent ?: return false
        if (::connectionsDao.isInitialized) {
            return false
        }
        intent.setExtrasClassLoader(classLoader)
        val dbProvider =
            intent.getParcelableExtra<MqttConnectionsDatabaseDescriptor>(MqttConnectionsDatabaseDescriptor.TAG)
                ?: return false
        connectionsDao = dbProvider.getDb(this).remoteHostsDao()
        return true
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

class BoundClientsObserver(newClientCb: (Messenger) -> Unit, val callback: (msg: Message) -> Unit) {
    private val registeredClients = LinkedHashSet<Messenger>()
    private val incomingHandler = MessageCallbackHandler {
        when (it.what) {
            REGISTER_CLIENT -> {
                registeredClients.add(it.replyTo)
                newClientCb(it.replyTo)
            }
            UNREGISTER_CLIENT -> registeredClients.remove(it.replyTo)
            else -> callback(it)
        }
    }
    val messenger = Messenger(incomingHandler)
    val binder = messenger.binder!!


    fun sendMessageToClients(msg: Message) = LinkedHashSet(registeredClients).forEach {
        try {
            it.send(msg)
        } catch (e: RemoteException) {
            // unregister the client, there is nothing we can do at this point as the other process has crashed
            registeredClients.remove(it)
        }
    }
}

const val REGISTER_CLIENT = Int.MIN_VALUE
const val UNREGISTER_CLIENT = Int.MIN_VALUE + 1

