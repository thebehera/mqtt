package mqtt.android_app

import android.app.Application
import androidx.room.*
import mqtt.Parcelize
import mqtt.androidx.room.MqttDatabase
import mqtt.client.service.MqttDatabaseDescriptor
import mqtt.client.service.MqttRoomDatabase
import mqtt.client.service.ipc.AbstractMqttServiceViewModel

@Entity
data class SimpleModel(val stringValue: String, @PrimaryKey(autoGenerate = true) val key: Long = 0)

@Dao
interface ModelsDao {
    @Insert
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

@Parcelize
object MqttDbProvider : MqttDatabaseDescriptor<SimpleModelDb>(Mqtt_RoomDb_SimpleModelDb::class.java)

class MqttServiceViewModel(app: Application) : AbstractMqttServiceViewModel(app, MqttDbProvider)