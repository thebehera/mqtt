package mqtt.client.service

import android.content.Context
import android.os.Parcelable
import androidx.room.Room
import androidx.room.RoomDatabase
import mqtt.client.persistence.IMqttConnectionsDb
import mqtt.client.persistence.QueuedObjectCollection
import kotlin.coroutines.CoroutineContext

abstract class MqttRoomDatabase : RoomDatabase(), IMqttConnectionsDb

interface MqttConnectionsDatabaseDescriptor : Parcelable {
    fun getDb(context: Context): MqttRoomDatabase
    fun getPersistence(
        context: Context,
        coroutineContext: CoroutineContext,
        connectionIdentifier: Int
    ): QueuedObjectCollection

    companion object {
        val TAG = MqttConnectionsDatabaseDescriptor::class.java.canonicalName!!
    }
}

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