package mqtt.client.service

import kotlinx.coroutines.CoroutineScope
import mqtt.client.MqttClient
import mqtt.connection.IMqttConfiguration
import mqtt.persistence.IQueuedMessage
import mqtt.persistence.MqttPersistence
import kotlin.coroutines.CoroutineContext

class ConnectionManager(connectionParameters: IMqttConfiguration, coroutineContext: CoroutineContext) {
    val client = MqttClient(connectionParameters)
    val queueManager = ConnectedQueueManager(coroutineContext, client)

    suspend fun connectAsync() = client.connectAsync().await()

}

class ConnectedQueueManager(override val coroutineContext: CoroutineContext, val client: MqttClient) :
    OnQueueInvalidatedListener, CoroutineScope {
    override fun onQueueInvalidated(persistence: MqttPersistence<out IQueuedMessage>) {
        val message = persistence.peekQueuedMessages(1).firstOrNull() ?: return

    }
}
