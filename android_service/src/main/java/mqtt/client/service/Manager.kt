package mqtt.client.service

import mqtt.connection.IMqttConfiguration

class ConnectionManager : OnConnectionListener {
    override fun connect(connectionParameters: IMqttConfiguration) {

    }
}

class ConnectedQueueManager : OnQueueInvalidatedListener {
    override fun onQueueInvalidated() {

    }
}
