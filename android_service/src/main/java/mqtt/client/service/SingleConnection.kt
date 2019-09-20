package mqtt.client.service

import android.content.Intent
import android.os.Bundle
import android.os.Message
import android.os.Parcelable
import android.util.Log
import kotlinx.coroutines.launch
import mqtt.client.service.ipc.BoundClientsObserver
import mqtt.client.service.ipc.ServiceToBoundClient.CONNECTION_STATE_CHANGED
import mqtt.connection.IMqttConfiguration

private const val TAG = "[MQTT][SiCo]"
const val MESSAGE_PAYLOAD = "msg_payload"

class SingleConnection : CoroutineService() {

    private lateinit var connectionManager: ConnectionManager
    override fun onBind(intent: Intent) = boundClients.binder
    private val boundClients = BoundClientsObserver { messageFromBoundClient ->
        messageFromBoundClient.data?.classLoader = classLoader
        val obj = messageFromBoundClient.data?.getParcelable<Parcelable>(MESSAGE_PAYLOAD)
            ?: return@BoundClientsObserver
        handleMessage(obj)
    }

    fun handleMessage(data: Parcelable) {
        Log.i("RAHUL", "MSG RECIEVED FROM CLIENT $data")
        launch {
            when (data) {
                is IMqttConfiguration -> {
                    connect(data)
                }
            }
        }
    }
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        intent?.setExtrasClassLoader(classLoader)
        val msg = intent?.getParcelableExtra<Parcelable>(MESSAGE_PAYLOAD)
        if (msg != null) {
            handleMessage(msg)
        }
        return super.onStartCommand(intent, flags, startId)
    }

    private suspend fun connect(connectionParameters: IMqttConfiguration) {
        connectionManager = ConnectionManager(connectionParameters)
        connectionManager.connect {
            val msg = Message.obtain(null, CONNECTION_STATE_CHANGED.ordinal)
            val bundle = Bundle()
            bundle.putParcelable(MESSAGE_PAYLOAD, it)
            msg.data = bundle
            boundClients.sendMessageToClients(msg)
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
