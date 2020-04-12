@file:Suppress("EXPERIMENTAL_API_USAGE")

package mqtt.android_app

import androidx.room.*
import mqtt.androidx.room.*
import mqtt.buffer.ReadBuffer
import mqtt.buffer.WriteBuffer
import mqtt.client.persistence.MqttRoomDatabase
import mqtt.wire.control.packet.MqttSerializable
import mqtt.wire.data.utf8Length

@Entity
@MqttPublish(defaultTopic = "simple")
data class SimpleModel(val stringValue: String, @PrimaryKey(autoGenerate = true) val key: Long = 0) {

    @MqttPublishSize
    @Ignore
    val bytePacketSize = Long.SIZE_BYTES + stringValue.utf8Length()
}

@MqttSerializer
object SimpleModelSerializer : MqttSerializable<SimpleModel> {
    override fun serialize(obj: SimpleModel, writeBuffer: WriteBuffer) {
        writeBuffer.write(obj.key)
        writeBuffer.writeUtf8String(obj.stringValue)
    }

    override fun deserialize(readBuffer: ReadBuffer) = with(readBuffer) {
        val key = readLong()
        val string = readMqttUtf8StringNotValidated().toString()
        SimpleModel(string, key)
    }
}

@Dao
interface ModelsDao {
    @Insert
    @MqttPublishQueue
    suspend fun insert(model: SimpleModel): Long

    @MqttPublishDequeue
    @Query("SELECT * FROM SimpleModel WHERE _rowid_ = :rowId")
    suspend fun getByRowId(rowId: Long): SimpleModel?

    @Query("DELETE FROM SimpleModel WHERE `key` = :key")
    suspend fun delete(key: Int)
}


@MqttDatabase(db = Database(entities = [SimpleModel::class], version = 1))
abstract class SimpleModelDb : MqttRoomDatabase() {
    abstract fun modelsDao(): ModelsDao
}
