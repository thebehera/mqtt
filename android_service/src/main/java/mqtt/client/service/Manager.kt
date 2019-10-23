package mqtt.client.service

import android.util.Log
import mqtt.client.MqttClient
import mqtt.client.persistence.QueuedObjectCollection
import mqtt.client.transport.OnMessageReceivedCallback
import mqtt.connection.ConnectionState
import mqtt.connection.IRemoteHost
import mqtt.connection.Initializing
import mqtt.connection.MqttConnectionStateUpdated
import mqtt.wire.control.packet.ControlPacket

class ConnectionManager(
    val remoteHost: IRemoteHost, queuedObjectCollection: QueuedObjectCollection,
    val cb: ((ControlPacket, Int) -> Unit)? = null
) :
    OnMessageReceivedCallback {

    val client = MqttClient(remoteHost, this, queuedObjectCollection)
    var connectionState: ConnectionState = Initializing

    suspend fun connect(connectionChangeCallback: ((MqttConnectionStateUpdated) -> Unit)) {
        client.startAsync {
            connectionState = it
            connectionChangeCallback(MqttConnectionStateUpdated(remoteHost, it))
        }
    }

    override fun onMessage(controlPacket: ControlPacket) {
        Log.i("RAHUL", "On Msg: $controlPacket")
        cb?.invoke(controlPacket, remoteHost.connectionIdentifier())
    }

    fun disconnectAsync() = client.disconnectAsync()
}