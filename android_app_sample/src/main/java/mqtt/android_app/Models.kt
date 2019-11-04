package mqtt.android_app

import androidx.room.*
import kotlinx.io.core.buildPacket
import mqtt.androidx.room.*
import mqtt.client.service.MqttRoomDatabase
import mqtt.wire.data.utf8Length

@Entity
@MqttPublish(defaultTopic = "simple/1")
data class SimpleModel(val stringValue: String, @PrimaryKey(autoGenerate = true) val key: Long = 0) {

    @MqttPublishSize
    @Ignore
    val bytePacketSize = Long.SIZE_BYTES + stringValue.utf8Length()

    @MqttPublishPacket
    fun toByteReadPacket() = buildPacket {
        writeLong(key)
        writeStringUtf8(stringValue)
    }
}

@Dao
interface ModelsDao {
    @Insert
    @MqttSubscribe("simple/+")
    suspend fun insert(model: SimpleModel): Long

    @Query("SELECT * FROM SimpleModel WHERE _rowid_ = :rowId")
    suspend fun getByRowId(rowId: Long): SimpleModel?

    @Query("DELETE FROM SimpleModel WHERE `key` = :key")
    suspend fun delete(key: Int)
}


@MqttDatabase(db = Database(entities = [SimpleModel::class], version = 1))
abstract class SimpleModelDb : MqttRoomDatabase() {
    abstract fun modelsDao(): ModelsDao
}
