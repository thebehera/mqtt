package mqtt.client.service

import mqtt.client.MqttClient
import mqtt.client.transport.OnMessageReceivedCallback
import mqtt.connection.ConnectionState
import mqtt.connection.IMqttConfiguration
import mqtt.connection.Initializing
import mqtt.connection.MqttConnectionStateUpdated
import mqtt.wire.control.packet.ControlPacket

class ConnectionManager(val connectionParameters: IMqttConfiguration, val cb: ((ControlPacket, Int) -> Unit)? = null) :
    OnMessageReceivedCallback {

    val client = MqttClient(connectionParameters, this)
    var connectionState: ConnectionState = Initializing

    suspend fun connect(connectionChangeCallback: ((MqttConnectionStateUpdated) -> Unit)) {
        client.startAsync {
            connectionState = it
            connectionChangeCallback(MqttConnectionStateUpdated(connectionParameters.remoteHost, it))
        }.await()
    }

    override fun onMessage(controlPacket: ControlPacket) {
        cb?.invoke(controlPacket, connectionParameters.remoteHost.connectionIdentifier())
    }

    fun disconnectAsync() = client.disconnectAsync()
}