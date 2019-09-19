package mqtt.client.service

import android.content.Intent
import android.os.Message
import android.util.Log
import kotlinx.coroutines.launch
import mqtt.client.service.ipc.BoundClientsObserver
import mqtt.client.service.ipc.EventHandler
import mqtt.client.service.ipc.ServiceToBoundClient.CONNECTION_STATE_CHANGED
import mqtt.connection.IMqttConfiguration

private const val TAG = "[MQTT][SiCo]"

class SingleConnection : CoroutineService() {

    private lateinit var connectionManager: ConnectionManager
    private val eventHandler = EventHandler(coroutineContext)
    override fun onBind(intent: Intent) = boundClients.binder
    private val boundClients = BoundClientsObserver { messageFromBoundClient ->
        val data = messageFromBoundClient.data
        val obj = messageFromBoundClient.obj
        Log.i("RAHUL", "MSG RECIEVED FROM CLIENT $obj")
        launch {
            when (obj) {
                is IMqttConfiguration -> {
                    connect(obj)
                }
            }
        }
        eventHandler.handleMessage(messageFromBoundClient)
    }

    init {
        Log.i("RAHUL", "Service Init")
    }

    override fun onCreate() {
        super.onCreate()
        Log.i("RAHUL", "On create")
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Log.i("RAHUL", "Start Command start")
        if (intent != null) {
            connectOrDisconnectViaIntent(intent)
        }
        try {
            return super.onStartCommand(intent, flags, startId)
        } finally {
            Log.i("RAHUL", "Start Command end")
        }
    }

    private fun connectOrDisconnectViaIntent(intent: Intent) = launch {
        if (!::connectionManager.isInitialized) {
            val configExtraName = IMqttConfiguration::class.java.canonicalName!!
            if (intent.hasExtra(configExtraName)) {
                val config = intent.getParcelableExtra<IMqttConfiguration>(configExtraName)
                connect(config)
            } else {
                // Android log because we don't have an instance of the logger yet
                android.util.Log.e(TAG, "Failed to find configuration data in intent: $intent")
            }
        } else {
            val log = connectionManager.connectionParameters.logConfiguration.getLogClass()
            log.warning(TAG, "Connection manager already initialized, ignoring connection intent $intent")
        }
    }

    private suspend fun connect(connectionParameters: IMqttConfiguration) {
        connectionManager = ConnectionManager(connectionParameters)
        connectionManager.connect {
            boundClients.sendMessageToClients(Message.obtain(null, CONNECTION_STATE_CHANGED.ordinal, it))
        }
    }

    private suspend fun disconnect() {
        connectionManager.disconnectAsync().await()
        stopSelf()
    }

    override fun onDestroy() {
        launch { disconnect() }
        super.onDestroy()
    }

}
