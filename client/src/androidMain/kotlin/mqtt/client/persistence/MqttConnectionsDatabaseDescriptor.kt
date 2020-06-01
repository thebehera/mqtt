package mqtt.client.persistence

import android.content.Context
import mqtt.client.MqttClient
import mqtt.wire.data.QualityOfService
import mqtt.wire.data.topic.Name
import kotlin.coroutines.CoroutineContext
import kotlin.reflect.KClass
import kotlin.time.ExperimentalTime

@ExperimentalTime
interface MqttConnectionsDatabaseDescriptor {
    fun getDb(context: Context): MqttRoomDatabase
    fun getPersistence(
        context: Context,
        coroutineContext: CoroutineContext,
        connectionIdentifier: Int
    ): QueuedObjectCollection

    suspend fun <T : Any> subscribe(
        client: MqttClient, packetId: UShort, topicOverride: String?,
        qosOverride: QualityOfService?, klass: KClass<T>,
        cb: (topic: Name, qos: QualityOfService, message: T?) -> Unit
    )

    companion object {
        val TAG = MqttConnectionsDatabaseDescriptor::class.java.canonicalName!!
    }
}