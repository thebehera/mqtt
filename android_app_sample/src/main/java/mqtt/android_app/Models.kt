package mqtt.android_app

import android.content.Context
import androidx.room.*
import mqtt.Parcelize
import mqtt.client.service.IMqttConnectionsDb
import mqtt.client.service.MqttConnectionsDatabaseDescriptor

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


@Database(entities = [SimpleModel::class], version = 1)
abstract class SimpleModelDb : RoomDatabase() {
    abstract fun modelsDao(): ModelsDao
}

@Parcelize
object MqttDbProvider : MqttConnectionsDatabaseDescriptor {
    lateinit var db: IMqttConnectionsDb

    override fun getDb(context: Context): IMqttConnectionsDb {
        if (::db.isInitialized) {
            return db
        }
        throw IllegalArgumentException("Y")
//        db = Room.databaseBuilder(context, SimpleModelDb::class.java, "simpleModels.db").build()
//        return db
    }
}