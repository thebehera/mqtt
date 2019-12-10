package mqtt.client.persistence

import android.os.Parcelable
import androidx.room.*
import mqtt.Parcelize
import mqtt.wire.control.packet.ISubscribeRequest
import mqtt.wire.data.QualityOfService

@Entity(primaryKeys = ["connectionIdentifier", "packetIdentifier"])
@TypeConverters(MqttV4TypeConverters::class)
@Parcelize
data class MqttQueue(
    val queuedType: String,
    val queuedRowId: Long,
    val controlPacketType: Byte,
    val qos: QualityOfService = QualityOfService.AT_LEAST_ONCE,
    val connectionIdentifier: Int,
    val bytesTransmitted: Int = 0,
    val packetIdentifier: Int = 0,
    val acknowledged: Boolean = false
) : Parcelable

@Entity(primaryKeys = ["connectionIdentifier", "packetIdentifier"])
@TypeConverters(MqttV4TypeConverters::class)
@Parcelize
data class MqttPublishQueue(
    val connectionIdentifier: Int,
    val packetIdentifier: Int = 0,
    val topic: String,
    val dup: Boolean = false,
    val retain: Boolean = false
) : Parcelable

@Dao
interface PersistedMqttQueueDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun queue(queue: MqttQueue): Long

    @Query("SELECT * FROM MqttQueue WHERE connectionIdentifier = :connectionIdentifier AND acknowledged = 0 ORDER BY packetIdentifier LIMIT 1")
    suspend fun getNext(connectionIdentifier: Int): MqttQueue?

    @Query("UPDATE MqttQueue SET acknowledged = 1 WHERE packetIdentifier = :messageId")
    suspend fun acknowledge(messageId: Int)

    @Query("SELECT * FROM MqttQueue WHERE _rowId_ = :rowId")
    suspend fun getQueuedObjectByRowId(rowId: Long): MqttQueue?

    @Query("SELECT * FROM MqttQueue WHERE connectionIdentifier = :connectionIdentifier  AND packetIdentifier = :messageId AND acknowledged = 0 LIMIT 1")
    suspend fun getByMessageId(messageId: Int, connectionIdentifier: Int): MqttQueue?

    @Insert
    suspend fun insertPublishQueue(mqttPublishQueue: MqttPublishQueue)

    @Query("SELECT * FROM MqttPublishQueue WHERE packetIdentifier = :messageId AND connectionIdentifier = :connectionIdentifier LIMIT 1")
    suspend fun getPublishQueue(connectionIdentifier: Int, messageId: Int): MqttPublishQueue?

    @Query("SELECT packetIdentifier FROM MqttQueue WHERE connectionIdentifier = :connectionIdentifier ORDER BY packetIdentifier DESC LIMIT 1")
    suspend fun largestMessageId(connectionIdentifier: Int): Int?

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun subscribe(subscription: MqttSubscription): Long

    @Query("SELECT * FROM MqttSubscription WHERE _rowId_ = :rowId")
    suspend fun getSubscription(rowId: Long): MqttSubscription?

    @Query("DELETE FROM MqttSubscription WHERE connectionIdentifier = :connectionIdentifier AND topicFilter = :topicFilter")
    suspend fun unsubscribe(connectionIdentifier: Int, topicFilter: String)

    @Transaction
    suspend fun subscribe(queuedType: String, qos: QualityOfService, subscription: MqttSubscription): MqttQueue {
        val nextPacketId = nextPacketId(this, subscription.connectionIdentifier)
        val mqttSubscription = subscription.copy(packetIdentifier = nextPacketId)
        val subscriptionRowId = subscribe(mqttSubscription)
        val queue = MqttQueue(
            queuedType,
            subscriptionRowId,
            ISubscribeRequest.controlPacketValue,
            qos,
            mqttSubscription.connectionIdentifier,
            packetIdentifier = nextPacketId
        )
        val queuedRowId = queue(queue)
        return queue.copy(queuedRowId = queuedRowId)
    }

    @Transaction
    suspend fun publish(queue: MqttQueue, topic: String, dup: Boolean, retain: Boolean): Int {
        val largestMsgIdNullable = largestMessageId(queue.connectionIdentifier)
        val nextMsgId = if (largestMsgIdNullable == null) {
            1
        } else {
            largestMsgIdNullable.rem(65_535) + 1
        }
        val copied = queue.copy(packetIdentifier = nextMsgId)
        val msgId = queue(copied).toInt()
        insertPublishQueue(MqttPublishQueue(queue.connectionIdentifier, msgId, topic, dup, retain))
        return nextMsgId
    }

    companion object {
        suspend fun nextPacketId(dao: PersistedMqttQueueDao, connectionIdentifier: Int): Int {
            val largestMsgIdNullable = dao.largestMessageId(connectionIdentifier)
            return if (largestMsgIdNullable == null) {
                1
            } else {
                largestMsgIdNullable.rem(65_535) + 1
            }
        }
    }
}