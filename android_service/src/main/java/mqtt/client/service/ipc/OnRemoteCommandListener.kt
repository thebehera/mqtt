package mqtt.client.service.ipc

import mqtt.client.connection.ConnectionParameters

interface OnRemoteCommandListener {

    fun connect(connectionParameters: ConnectionParameters)
    fun onQueueInvalidated()
}