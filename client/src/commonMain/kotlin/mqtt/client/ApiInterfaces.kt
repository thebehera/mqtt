package mqtt.client

import kotlinx.coroutines.Deferred
import mqtt.connection.IRemoteHost
import mqtt.wire.data.QualityOfService
import mqtt.wire.data.topic.Name
import kotlin.reflect.KClass

interface SimpleMqttClient {
    val remoteHost: IRemoteHost
    fun connectAsync(): Deferred<Any>?
    suspend fun <T : Any> publish(topic: String, qos: QualityOfService, typeClass: KClass<T>, obj: T): Unit
    suspend fun <T : Any> subscribe(
        topicFilter: String, qos: QualityOfService, typeClass: KClass<T>,
        callback: (topic: Name, qos: QualityOfService, message: T?) -> Unit
    ): Unit

    fun disconnectAsync(): Deferred<Boolean>?
}
