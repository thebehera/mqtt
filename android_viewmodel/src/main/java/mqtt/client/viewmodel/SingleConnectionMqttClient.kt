package mqtt.client.viewmodel

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import mqtt.client.MqttClient
import mqtt.client.connection.ConnectionParameters
import mqtt.wire.data.QualityOfService
import mqtt.wire.data.topic.Name

class SingleConnectionMqttClient : ViewModel(), CoroutineScope {
    private val job: Job = Job()
    override val coroutineContext = job + Dispatchers.Main
    var client: MqttClient? = null

    fun connectAsync(parameters: ConnectionParameters): Deferred<Any> {
        if (client != null) throw IllegalStateException("Client already exists!")
        val client = MqttClient(parameters)
        this.client = client
        return client.connectAsync()
    }

    suspend inline fun <reified T : Any> publish(topic: String, qos: QualityOfService, obj: T) =
        client?.publish(topic, qos, obj)

    suspend inline fun <reified T : Any> subscribe(
        topicFilter: String, qos: QualityOfService,
        crossinline callback: (topic: Name, qos: QualityOfService, message: T?) -> Unit
    ) = client?.subscribe(topicFilter, qos, callback)

    fun disconnect() {
        client?.disconnectAsync()
        client = null
    }

    override fun onCleared() {
        disconnect()
    }
}