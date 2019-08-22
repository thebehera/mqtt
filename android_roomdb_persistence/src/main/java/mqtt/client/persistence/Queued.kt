@file:Suppress("EXPERIMENTAL_API_USAGE")

package mqtt.client.persistence

import androidx.room.*
import mqtt.persistence.IQueuedMessage
import mqtt.persistence.MqttPersistence
import mqtt.persistence.QueuedObjectId

const val TABLE_NAME = "mqtt.persistence.IQueuedMessage"

@Entity(tableName = TABLE_NAME)
data class QueuedMessage(
    @PrimaryKey
    override val childTableName: String,
    override val childRowId: Long,
    override val priority: Double = 0.0,
    // Assigned by mqtt internals
    @TypeConverters(RoomTypeConverters::class)
    override val messageId: Int? = null,
    // Assigned by SQLite
    override val queuedObjectId: QueuedObjectId = 0
) : IQueuedMessage

@Dao
interface MqttQueuedDao : MqttPersistence<QueuedMessage> {

    @Query("INSERT INTO `$TABLE_NAME` (childTableName, childRowId, priority) VALUES(:tableName, :rowId, :priority)")
    override fun queueNewMessage(tableName: String, rowId: Long, priority: Double): QueuedObjectId

    @Query("SELECT * FROM `$TABLE_NAME` ORDER BY priority DESC, queuedObjectId ASC LIMIT :count")
    override fun peekQueuedMessages(count: Int): List<QueuedMessage>

    @Query("DELETE FROM `$TABLE_NAME` WHERE queuedObjectId = :queuedObjectId")
    override fun attemptCancel(queuedObjectId: QueuedObjectId)

    @Query("DELETE FROM `$TABLE_NAME` WHERE messageId = :messageId")
    override fun acknowlege(messageId: Int)
}


@Database(entities = [QueuedMessage::class], version = 1)
@TypeConverters(RoomTypeConverters::class)
abstract class QueuedDb : RoomDatabase() {
    abstract fun mqttQueued(): MqttQueuedDao
}
