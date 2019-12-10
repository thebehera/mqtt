package mqtt.client.persistence

import android.content.Context
import androidx.room.Room
import androidx.room.RoomDatabase

abstract class MqttDatabaseDescriptor<Database : MqttRoomDatabase>(
    private val type: Class<out Database>,
    val dbName: String = "mqtt.db"
) : MqttConnectionsDatabaseDescriptor {
    lateinit var db: Database

    open fun onBuildDatabase(it: RoomDatabase.Builder<out Database>) {}

    override fun getDb(context: Context): Database {
        if (::db.isInitialized) {
            return db
        }
        db = Room.databaseBuilder(context, type, dbName).also {
            onBuildDatabase(it)
            it.enableMultiInstanceInvalidation()
        }.build()
        return db
    }
}