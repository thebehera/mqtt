package mqtt.client

import mqtt.client.persistence.QueuedObjectCollection
import mqtt.connection.IRemoteHost

data class MqttClient2(val remoteHost: IRemoteHost, val queuedObjectCollection: QueuedObjectCollection)