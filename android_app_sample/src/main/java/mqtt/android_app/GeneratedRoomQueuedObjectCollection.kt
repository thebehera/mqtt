package mqtt.android_app

import mqtt.client.persistence.RoomQueuedObjectCollection
import mqtt.wire.control.packet.ControlPacket
import mqtt.wire4.control.packet.PublishMessage
import kotlin.coroutines.CoroutineContext

class GeneratedRoomQueuedObjectCollection(
    val connectionIdentifier: Int,
    override val db: Mqtt_RoomDb_SimpleModelDb, override val coroutineContext: CoroutineContext
) : RoomQueuedObjectCollection(db, connectionIdentifier) {
    val mqttDao = db.mqttQueueDao()

    override suspend fun get(packetId: Int?): ControlPacket? {
        val queuedObj = nextQueuedObj(packetId) ?: return null
        val publishQueue =
            mqttDao.getPublishQueue(queuedObj.connectionIdentifier, queuedObj.packetIdentifier) ?: return null
        when (queuedObj.queuedType) {
            SimpleModel::class.java.simpleName -> {
                val obj = db.modelsDao().getByRowId(queuedObj.queuedRowId) ?: return null
                return PublishMessage(
                    publishQueue.topic,
                    queuedObj.qos,
                    obj.toByteReadPacket(),
                    publishQueue.packetIdentifier.toUShort(),
                    publishQueue.dup,
                    publishQueue.retain
                )
            }
            String::class.java.simpleName -> {
                return null
            }
        }
        return null
    }
}