package mqtt.client.persistence

import android.os.Parcelable
import androidx.room.*
import mqtt.Parcelize
import mqtt.client.connection.parameters.MqttV4TypeConverters
import mqtt.client.connection.parameters.RemoteHostDao
import mqtt.wire.data.QualityOfService

@Entity(primaryKeys = ["connectionIdentifier", "messageId"])
@TypeConverters(MqttV4TypeConverters::class)
@Parcelize
data class MqttQueue(
    val queuedType: String,
    val queuedRowId: Long,
    val controlPacketType: Int,
    val connectionIdentifier: Int,
    val bytesSent: Int = 0,
    val messageId: Int = 0,
    val acknowleged: Boolean = false
) : Parcelable

@Entity(primaryKeys = ["connectionIdentifier", "messageId"])
@TypeConverters(MqttV4TypeConverters::class)
@Parcelize
data class MqttPublishQueue(
    val connectionIdentifier: Int,
    val messageId: Int,
    val topic: String,
    val qos: QualityOfService,
    val dup: Boolean = false,
    val retain: Boolean = false
) : Parcelable

@Dao
interface PersistedMqttQueueDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun queue(queue: MqttQueue): Long

    @Query("SELECT * FROM MqttQueue WHERE connectionIdentifier = :connectionIdentifier AND acknowleged = 0 ORDER BY messageId LIMIT 1")
    suspend fun getNext(connectionIdentifier: Int): MqttQueue?

    @Query("UPDATE MqttQueue SET acknowleged = 1 WHERE messageId = :messageId")
    suspend fun acknowledge(messageId: Int)

    @Query("SELECT * FROM MqttQueue WHERE connectionIdentifier = :connectionIdentifier  AND messageId = :messageId  LIMIT 1")
    suspend fun getByMessageId(messageId: Int, connectionIdentifier: Int): MqttQueue?

    @Insert
    suspend fun insertPublishQueue(mqttPublishQueue: MqttPublishQueue)

    @Query("SELECT * FROM MqttPublishQueue WHERE messageId = :messageId AND connectionIdentifier = :connectionIdentifier LIMIT 1")
    suspend fun getPublishQueue(connectionIdentifier: Int, messageId: Int): MqttPublishQueue?

    @Query("SELECT messageId FROM MqttQueue WHERE connectionIdentifier = :connectionIdentifier ORDER BY messageId DESC LIMIT 1")
    suspend fun nextLargestMessageId(connectionIdentifier: Int): Int?

    @Transaction
    suspend fun publish(queue: MqttQueue, topic: String, qos: QualityOfService, dup: Boolean, retain: Boolean): Int {
        val nextMsgId = nextLargestMessageId(queue.connectionIdentifier)?.rem(65_535) ?: 1
        val copied = queue.copy(messageId = nextMsgId)
        val msgId = queue(copied).toInt()
        insertPublishQueue(MqttPublishQueue(queue.connectionIdentifier, msgId, topic, qos, dup, retain))
        return nextMsgId
    }
}


interface IMqttConnectionsDb {
    fun remoteHostsDao(): RemoteHostDao
    fun mqttQueueDao(): PersistedMqttQueueDao
}