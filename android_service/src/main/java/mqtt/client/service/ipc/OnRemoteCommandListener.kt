package mqtt.client.service.ipc

import mqtt.client.connection.parameters.IMqttConfiguration

interface OnRemoteCommandListener {
    fun connect(connectionParameters: IMqttConfiguration)
    fun onQueueInvalidated()
}