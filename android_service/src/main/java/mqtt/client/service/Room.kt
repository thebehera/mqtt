package mqtt.client.service

import android.content.Context
import android.os.Parcelable
import androidx.room.Room
import androidx.room.RoomDatabase
import mqtt.client.connection.parameters.RemoteHostDao

interface IMqttConnectionsDb {
    fun remoteHostsDao(): RemoteHostDao
}

abstract class MqttRoomDatabase : RoomDatabase(), IMqttConnectionsDb


interface MqttConnectionsDatabaseDescriptor : Parcelable {
    fun getDb(context: Context): IMqttConnectionsDb

    companion object {
        val TAG = MqttConnectionsDatabaseDescriptor::class.java.canonicalName!!
    }
}

abstract class MqttDatabaseDescriptor<Database : MqttRoomDatabase>(private val type: Class<out MqttRoomDatabase>) :
    MqttConnectionsDatabaseDescriptor {
    lateinit var db: Database

    override fun getDb(context: Context): Database {
        if (::db.isInitialized) {
            return db
        }
        db = Room.databaseBuilder(context, type, "mqtt.db").build() as Database
        return db
    }
}