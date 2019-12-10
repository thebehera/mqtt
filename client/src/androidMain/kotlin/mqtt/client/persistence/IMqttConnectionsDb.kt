package mqtt.client.persistence

interface IMqttConnectionsDb {
    fun remoteHostsDao(): RemoteHostDao
    fun mqttQueueDao(): PersistedMqttQueueDao
}