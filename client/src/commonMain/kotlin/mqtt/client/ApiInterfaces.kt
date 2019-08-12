package mqtt.client

import kotlinx.coroutines.Deferred
import mqtt.client.connection.parameters.IMqttConfiguration
import mqtt.wire.data.QualityOfService
import mqtt.wire.data.topic.Name
import kotlin.reflect.KClass

interface SingleMqttClientApi {
    fun connectAsync(parameters: IMqttConfiguration): Deferred<Any>?
    suspend fun <T : Any> publish(topic: String, qos: QualityOfService, typeClass: KClass<T>, obj: T): Unit
    suspend fun <T : Any> subscribe(
        topicFilter: String, qos: QualityOfService, typeClass: KClass<T>,
        callback: (topic: Name, qos: QualityOfService, message: T?) -> Unit
    ): Unit

    fun disconnect(): Deferred<Boolean>?
}