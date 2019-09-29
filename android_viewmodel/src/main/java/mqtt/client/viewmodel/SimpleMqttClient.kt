package mqtt.client.viewmodel

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.Deferred
import mqtt.client.MqttClient
import mqtt.client.SimpleMqttClient
import mqtt.connection.IRemoteHost
import mqtt.wire.data.QualityOfService
import mqtt.wire.data.topic.Name
import kotlin.reflect.KClass

class SimpleMqttClientViewModel : ViewModel() {
    lateinit var client: SimpleMqttClient


    fun connectAsync(parameters: IRemoteHost): Deferred<Any> {
        if (::client.isInitialized) throw IllegalStateException("Client already exists!")
        val client = MqttClient(parameters)
        this.client = client
        return client.connectAsync()
    }

    suspend inline fun <reified T : Any> publish(topic: String, qos: QualityOfService, obj: T) =
        publish(topic, qos, T::class, obj)

    suspend fun <T : Any> publish(topic: String, qos: QualityOfService, typeClass: KClass<T>, obj: T) {
        client.publish(topic, qos, typeClass, obj)
    }


    suspend inline fun <reified T : Any> subscribe(
        topicFilter: String, qos: QualityOfService,
        noinline callback: (topic: Name, qos: QualityOfService, message: T?) -> Unit
    ) = client.subscribe(topicFilter, qos, T::class, callback)

    suspend fun <T : Any> subscribe(
        topicFilter: String, qos: QualityOfService, typeClass: KClass<T>,
        callback: (topic: Name, qos: QualityOfService, message: T?) -> Unit
    ) {
        client.subscribe(topicFilter, qos, typeClass, callback)
    }


    fun disconnect(): Deferred<Boolean>? {
        val deferred = client.disconnectAsync()
        return deferred
    }

    @Suppress("DeferredResultUnused")
    override fun onCleared() {
        disconnect()
    }
}
