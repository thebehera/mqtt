package mqtt.client.viewmodel

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import mqtt.client.MqttClient
import mqtt.client.SingleMqttClientApi
import mqtt.client.connection.parameters.IMqttConfiguration
import mqtt.wire.data.QualityOfService
import mqtt.wire.data.topic.Name
import kotlin.reflect.KClass

class SingleConnectionMqttClient : ViewModel(), SingleMqttClientApi, CoroutineScope {
    private val job: Job = Job()
    override val coroutineContext = job + Dispatchers.Main
    var client: MqttClient? = null

    override fun connectAsync(parameters: IMqttConfiguration): Deferred<Any> {
        if (client != null) throw IllegalStateException("Client already exists!")
        val client = MqttClient(parameters)
        this.client = client
        return client.connectAsync()
    }

    suspend inline fun <reified T : Any> publish(topic: String, qos: QualityOfService, obj: T) =
        publish(topic, qos, T::class, obj)

    override suspend fun <T : Any> publish(topic: String, qos: QualityOfService, typeClass: KClass<T>, obj: T) {
        client?.publish(topic, qos, typeClass, obj)
    }


    suspend inline fun <reified T : Any> subscribe(
        topicFilter: String, qos: QualityOfService,
        noinline callback: (topic: Name, qos: QualityOfService, message: T?) -> Unit
    ) = client?.subscribe(topicFilter, qos, T::class, callback)

    override suspend fun <T : Any> subscribe(
        topicFilter: String, qos: QualityOfService, typeClass: KClass<T>,
        callback: (topic: Name, qos: QualityOfService, message: T?) -> Unit
    ) {
        client?.subscribe(topicFilter, qos, typeClass, callback)
    }


    override fun disconnect(): Deferred<Boolean>? {
        val deferred = client?.disconnectAsync()
        client = null
        return deferred
    }

    @Suppress("DeferredResultUnused")
    override fun onCleared() {
        disconnect()
    }
}