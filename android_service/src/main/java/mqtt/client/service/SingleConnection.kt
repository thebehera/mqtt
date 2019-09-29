package mqtt.client.service

import android.content.Intent
import android.os.Bundle
import android.os.Message
import android.os.Parcelable
import kotlinx.coroutines.launch
import mqtt.client.service.ipc.BoundClientsObserver
import mqtt.client.service.ipc.ServiceToBoundClient
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
        launch {
            when (data) {
                is IMqttConfiguration -> {
                    connect(data)
                }
            }
        }
    }

    private suspend fun connect(connectionParameters: IMqttConfiguration) {
        connectionManager = ConnectionManager(connectionParameters) { controlPacket, remoteHostId ->
            val msg = Message.obtain()
            msg.what = ServiceToBoundClient.INCOMING_CONTROL_PACKET.ordinal
            msg.arg1 = remoteHostId
            val bundle = Bundle()
            bundle.putParcelable(MESSAGE_PAYLOAD, controlPacket)
            msg.data = bundle
            boundClients.sendMessageToClients(msg)
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
