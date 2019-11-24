package mqtt.android_app

import android.app.Application
import android.content.Context
import kotlinx.android.parcel.Parcelize
import mqtt.androidx.room.MqttGeneratedCodeException
import mqtt.client.MqttClient
import mqtt.client.persistence.MqttQueue
import mqtt.client.persistence.RoomQueuedObjectCollection
import mqtt.client.service.MqttDatabaseDescriptor
import mqtt.client.service.ipc.AbstractMqttServiceViewModel
import mqtt.client.service.ipc.ClientToServiceConnection
import mqtt.wire.control.packet.ControlPacket
import mqtt.wire.control.packet.IPublishMessage
import mqtt.wire.control.packet.installSerializer
import mqtt.wire.data.QualityOfService
import mqtt.wire.data.topic.Name
import mqtt.wire.data.topic.SubscriptionCallback
import mqtt.wire4.control.packet.PublishMessage
import kotlin.coroutines.CoroutineContext
import kotlin.reflect.KClass

@Parcelize
object MqttDbProvider : MqttDatabaseDescriptor<Mqtt_RoomDb_SimpleModelDb>(Mqtt_RoomDb_SimpleModelDb::class.java) {
    init {
        installSerializer(SimpleModelSerializer)
    }

    override fun getPersistence(context: Context, coroutineContext: CoroutineContext, connectionIdentifier: Int) =
        GeneratedRoomQueuedObjectCollection(connectionIdentifier, getDb(context), coroutineContext)

    override suspend fun <T : Any> subscribe(
        client: MqttClient, packetId: UShort, topicOverride: String?,
        qosOverride: QualityOfService?, klass: KClass<T>,
        cb: (topic: Name, qos: QualityOfService, message: T?) -> Unit
    ) {
        if (klass == SimpleModel::class) {
            val topicName = topicOverride ?: "simple"
            val qos = qosOverride ?: QualityOfService.AT_LEAST_ONCE  // injected value
            client.subscribe(topicName, qos, packetId, klass, cb)
        }
    }
}


class MqttServiceViewModelGenerated(private val app: Application) : AbstractMqttServiceViewModel(app, MqttDbProvider) {


    suspend inline fun <reified T : Any> subscribe(
        connectionIdentifier: Int, topicOverride: String? = null,
        qosOverride: QualityOfService? = null,
        crossinline cb: (topic: Name, qos: QualityOfService, message: T?) -> Unit
    ) {
        if (T::class == SimpleModel::class) {
            val topicName = topicOverride ?: "simple"
            val qos = qosOverride ?: QualityOfService.AT_LEAST_ONCE  // injected value
            subscribe(topicName, qos, connectionIdentifier, object : SubscriptionCallback<T> {
                override fun onMessageReceived(topic: Name, qos: QualityOfService, message: T?) {
                    cb(topic, qos, message)
                }
            })
        }
    }

    suspend fun <T : Any> publish(
        connectionIdentifier: Int, obj: T,
        topicOverride: String? = null, qosOverride: QualityOfService? = null,
        dupOverride: Boolean? = null, retainOverride: Boolean? = null
    ) {
        val db = MqttDbProvider.getDb(app)
        val rowId = when (obj) {
            is SimpleModel -> {
                val id = db.modelsDao().insert(obj)
                val modelWithId = obj.copy(key = id)

                val qos = qosOverride ?: QualityOfService.AT_LEAST_ONCE
                val queue = MqttQueue(
                    SimpleModel::class.java.simpleName,
                    modelWithId.key, IPublishMessage.controlPacketValue, qos, connectionIdentifier
                )
                db.mqttQueueDao().publish(
                    queue,
                    topicOverride ?: "simple",
                    dupOverride ?: false,
                    retainOverride ?: false
                ) //Inject the real value
            }
            else -> throw MqttGeneratedCodeException(
                "Failed to publish $obj. Did you forget to annotate" +
                        " ${obj::class.java.canonicalName} with @MqttPublish?"
            )
        }
        notifyPublish(ClientToServiceConnection.NotifyPublish(connectionIdentifier, rowId))
    }

}


class GeneratedRoomQueuedObjectCollection(
    val connectionIdentifier: Int,
    override val db: Mqtt_RoomDb_SimpleModelDb, override val coroutineContext: CoroutineContext
) : RoomQueuedObjectCollection(db, connectionIdentifier) {
    val mqttDao = db.mqttQueueDao()

    override suspend fun get(packetId: Int?): ControlPacket? {
        val queuedObj = nextQueuedObj(packetId) ?: return null
        if (queuedObj.queuedType == SimpleModel::class.java.simpleName) {
            val obj = db.modelsDao().getByRowId(queuedObj.queuedRowId) ?: return null
            val publishQueue =
                mqttDao.getPublishQueue(queuedObj.connectionIdentifier, queuedObj.packetIdentifier) ?: return null
            return PublishMessage(
                publishQueue.topic,
                queuedObj.qos,
                obj.toByteReadPacket(),
                publishQueue.packetIdentifier.toUShort(),
                publishQueue.dup,
                publishQueue.retain
            )
        }
        return null
    }
}