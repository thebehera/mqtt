package mqtt.client.service

import mqtt.client.MqttClient
import mqtt.connection.ConnectionState
import mqtt.connection.IMqttConfiguration
import mqtt.connection.Initializing
import mqtt.connection.MqttConnectionStateUpdated

class ConnectionManager(val connectionParameters: IMqttConfiguration) {
    val client = MqttClient(connectionParameters)
    var connectionState: ConnectionState = Initializing

    suspend fun connect(connectionChangeCallback: ((MqttConnectionStateUpdated) -> Unit)) {
        client.startAsync {
            connectionState = it
            connectionChangeCallback(MqttConnectionStateUpdated(connectionParameters.remoteHost, it))
        }.await()
    }

    fun disconnectAsync() = client.disconnectAsync()
}