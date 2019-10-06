package mqtt.client.connection.parameters

import androidx.room.*

@Entity
data class MqttQueue(
    val table: String,
    val tableRowId: Int,
    @PrimaryKey(autoGenerate = true)
    val messageId: Int = 0
)

@Dao
interface PersistedQueueDao {
    @Insert
    fun queue(queue: MqttQueue) = Int

    @Query("DELETE FROM MqttQueue WHERE messageId = :messageId")
    fun acknowledge(messageId: Int)
}


