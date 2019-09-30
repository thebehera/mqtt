package mqtt.client.service

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.os.Message
import android.os.Messenger
import android.os.Parcelable
import android.util.Log
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import kotlinx.coroutines.launch
import mqtt.client.connection.parameters.PersistableRemoteHostV4
import mqtt.client.connection.parameters.RemoteHostDao
import mqtt.client.service.ipc.BoundClientsObserver
import mqtt.client.service.ipc.ServiceToBoundClient
import mqtt.client.service.ipc.ServiceToBoundClient.CONNECTION_STATE_CHANGED
import mqtt.connection.IRemoteHost
import mqtt.connection.MqttConnectionStateUpdated

private const val TAG = "[MQTT][SiCo]"
const val MESSAGE_PAYLOAD = "msg_payload"

class ConnectionManagerService : CoroutineService() {
    override fun onBind(intent: Intent) = boundClients.binder
    private val connectionManagers = HashMap<Int, ConnectionManager>()

    private val boundClients by lazy {
        BoundClientsObserver(newClientCb) { messageFromBoundClient ->
            messageFromBoundClient.data?.classLoader = classLoader
            val obj = messageFromBoundClient.data?.getParcelable<Parcelable>(MESSAGE_PAYLOAD)
                ?: return@BoundClientsObserver
            handleMessage(obj)
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
    private val connectionsDao by lazy { mqttConnectionsDb.remoteHostsDao() }

    private fun handleMessage(data: Parcelable) = launch {
        when (data) {
            is IRemoteHost -> {
                connect(data)
            }
        }
    }

    override fun attachBaseContext(base: Context?) {
        super.attachBaseContext(base)
        initMqttConnectionsDb(base!!)
    }

    override fun onCreate() {
        super.onCreate()
        launch {
            Log.i("RAHUL", "Get all connections")
            connectionsDao.getAllConnections().forEach {
                Log.i("RAHUL", "Connect $it")
                connect(it)
            }
            Log.i("RAHUL", "Get all connections done")
        }
    }

    private suspend fun connect(connectionParameters: IRemoteHost) {
        Log.i("RAHUL", "Connect")
        if (connectionManagers[connectionParameters.connectionIdentifier()] != null) {
            Log.i("RAHUL", "Connect bail")
            return
        }
        val connectionManager = ConnectionManager(connectionParameters) { controlPacket, remoteHostId ->
            val msg = Message.obtain()
            msg.what = ServiceToBoundClient.INCOMING_CONTROL_PACKET.ordinal
            msg.arg1 = remoteHostId
            val bundle = Bundle()
            bundle.putParcelable(MESSAGE_PAYLOAD, controlPacket)
            msg.data = bundle
            boundClients.sendMessageToClients(msg)
        }
        Log.i("RAHUL", "Connect set mgr")
        connectionManagers[connectionParameters.connectionIdentifier()] = connectionManager
        Log.i("RAHUL", "Connect ${connectionParameters::class}")
        if (connectionParameters is PersistableRemoteHostV4) {
            connectionsDao.addOrUpdate(connectionParameters)
            Log.i("RAHUL", "updated $connectionParameters")
        }
        connectionManager.client.session.outboundCallback = { controlPacketSentToServer, remoteHostId ->
            val msg = Message.obtain()
            msg.what = ServiceToBoundClient.OUTGOING_CONTROL_PACKET.ordinal
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
        val msg = Message.obtain(null, CONNECTION_STATE_CHANGED.ordinal)
        val bundle = Bundle()
        bundle.putParcelable(MESSAGE_PAYLOAD, connectionStateUpdated)
        msg.data = bundle
        return msg
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


lateinit var appContext: Context

fun initMqttConnectionsDb(context: Context): MqttConnectionsDb {
    if (::appContext.isInitialized) {
        return mqttConnectionsDb
    }
    appContext = context.applicationContext
    return mqttConnectionsDb
}

@Database(entities = [PersistableRemoteHostV4::class], version = 1)
abstract class MqttConnectionsDb : RoomDatabase() {
    abstract fun remoteHostsDao(): RemoteHostDao
}

val mqttConnectionsDb by lazy {
    Room.databaseBuilder(appContext, MqttConnectionsDb::class.java, "mqttConnections.db").build()
}
