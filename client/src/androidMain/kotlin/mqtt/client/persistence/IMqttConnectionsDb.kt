package mqtt.client.persistence

import mqtt.client.connection.parameters.RemoteHostDao

interface IMqttConnectionsDb {
    fun remoteHostsDao(): RemoteHostDao
    fun mqttQueueDao(): PersistedMqttQueueDao
}