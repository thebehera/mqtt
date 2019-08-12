package mqtt.client.service

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.Deferred
import mqtt.client.SingleMqttClientApi
import mqtt.client.connection.parameters.IMqttConfiguration
import mqtt.wire.data.QualityOfService
import mqtt.wire.data.topic.Name
import kotlin.reflect.KClass

class SingleConnectionViewModel : ViewModel(), SingleMqttClientApi {
    override fun connectAsync(parameters: IMqttConfiguration): Deferred<Any>? {

        return null
    }

    override suspend fun <T : Any> publish(topic: String, qos: QualityOfService, typeClass: KClass<T>, obj: T) {

    }

    override suspend fun <T : Any> subscribe(
        topicFilter: String,
        qos: QualityOfService,
        typeClass: KClass<T>,
        callback: (topic: Name, qos: QualityOfService, message: T?) -> Unit
    ) {

    }

    override fun disconnect(): Deferred<Boolean>? {

        return null
    }

}