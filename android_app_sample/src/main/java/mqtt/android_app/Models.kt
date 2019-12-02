package mqtt.android_app

import androidx.room.*
import kotlinx.io.core.ByteReadPacket
import kotlinx.io.core.buildPacket
import kotlinx.io.streams.readerUTF8
import mqtt.androidx.room.*
import mqtt.client.service.MqttRoomDatabase
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
    override fun serialize(obj: SimpleModel) = buildPacket {
        writeLong(obj.key)
        writeStringUtf8(obj.stringValue)
    }

    override fun deserialize(buffer: ByteReadPacket) = with(buffer) {
        val key = readLong()
        val string = readerUTF8().use { readText() }
        SimpleModel(string, key)
    }
}

@Dao
interface ModelsDao {
    @Insert
    @MqttSubscribe("simple")
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
