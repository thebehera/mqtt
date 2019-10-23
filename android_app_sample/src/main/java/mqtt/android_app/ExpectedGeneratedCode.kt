package mqtt.android_app

import android.app.Application
import android.content.Context
import kotlinx.android.parcel.Parcelize
import mqtt.androidx.room.MqttGeneratedCodeException
import mqtt.client.persistence.MqttQueue
import mqtt.client.persistence.RoomQueuedObjectCollection
import mqtt.client.service.MqttDatabaseDescriptor
import mqtt.client.service.ipc.AbstractMqttServiceViewModel
import mqtt.client.service.ipc.ClientToServiceConnection
import mqtt.wire.control.packet.ControlPacket
import mqtt.wire.control.packet.IPublishMessage
import mqtt.wire.data.QualityOfService
import mqtt.wire4.control.packet.PublishMessage

@Parcelize
object MqttDbProvider : MqttDatabaseDescriptor<Mqtt_RoomDb_SimpleModelDb>(Mqtt_RoomDb_SimpleModelDb::class.java) {
    override fun getPersistence(context: Context, connectionIdentifier: Int) =
        GeneratedRoomQueuedObjectCollection(connectionIdentifier, getDb(context))
}

class MqttServiceViewModelGenerated(private val app: Application) : AbstractMqttServiceViewModel(app, MqttDbProvider) {
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

                val queue = MqttQueue(
                    SimpleModel::class.java.simpleName,
                    modelWithId.key, IPublishMessage.controlPacketValue, connectionIdentifier
                )
                db.mqttQueueDao().publish(
                    queue,
                    topicOverride ?: "{injectedDefaultTopic}",
                    qosOverride ?: QualityOfService.AT_LEAST_ONCE,
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
    connectionIdentifier: Int,
    override val db: Mqtt_RoomDb_SimpleModelDb
) : RoomQueuedObjectCollection(db, connectionIdentifier) {
    val mqttDao = db.mqttQueueDao()

    override suspend fun get(messageId: Int?): ControlPacket? {
        val queuedObj = nextQueuedObj(messageId) ?: return null
        if (queuedObj.queuedType == SimpleModel::class.java.simpleName) {
            val obj = db.modelsDao().getByRowId(queuedObj.queuedRowId) ?: return null
            val publishQueue =
                mqttDao.getPublishQueue(queuedObj.connectionIdentifier, queuedObj.messageId) ?: return null
            return PublishMessage(
                publishQueue.topic,
                publishQueue.qos,
                obj.toByteReadPacket(),
                publishQueue.messageId.toUShort(),
                publishQueue.dup,
                publishQueue.retain
            )
        }
        return null
    }
}