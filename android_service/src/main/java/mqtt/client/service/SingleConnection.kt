package mqtt.client.service

import android.content.Intent
import kotlinx.coroutines.launch
import mqtt.client.service.ipc.BoundClientsObserver
import mqtt.client.service.ipc.EventHandler
import mqtt.connection.IMqttConfiguration

private const val TAG = "[MQTT][SiCo]"

class SingleConnection : CoroutineService(), OnRemoteIpcCommand {

    private lateinit var connectionManager: ConnectionManager
    private val eventHandler = EventHandler(coroutineContext, this)
    override fun onBind(intent: Intent) = boundClients.binder
    private val boundClients = BoundClientsObserver {
        val data = it.data
        val obj = it.obj
        launch {
            when (obj) {
                is IMqttConfiguration -> {
                    connect(obj)

                }
            }
        }
        eventHandler.handleMessage(it)
        it.toString()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent != null) {
            connectOrDisconnectViaIntent(intent)
        }
        return super.onStartCommand(intent, flags, startId)
    }

    private fun connectOrDisconnectViaIntent(intent: Intent) = launch {
        if (!::connectionManager.isInitialized) {
            val configExtraName = IMqttConfiguration::class.java.canonicalName!!
            if (intent.hasExtra(configExtraName)) {
                val config = intent.getParcelableExtra<IMqttConfiguration>(configExtraName)
                connectOrDisconnect(config)
            } else {
                // Android log because we don't have an instance of the logger yet
                android.util.Log.e(TAG, "Failed to find configuration data in intent: $intent")
            }
        } else {
            val log = connectionManager.connectionParameters.logConfiguration.getLogClass()
            log.warning(TAG, "Connection manager already initialized, ignoring connection intent $intent")
        }
    }

    override suspend fun connectOrDisconnect(connectionParameters: IMqttConfiguration?) {
        if (connectionParameters == null) {
            disconnect()
        } else {
            connect(connectionParameters)
        }
    }

    private suspend fun connect(connectionParameters: IMqttConfiguration) {
        connectionManager = ConnectionManager(connectionParameters, coroutineContext)
        connectionManager.connectAsync().await()
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
