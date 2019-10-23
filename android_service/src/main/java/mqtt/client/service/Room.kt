package mqtt.client.service

import android.content.Context
import android.os.Parcelable
import androidx.room.Room
import androidx.room.RoomDatabase
import mqtt.client.connection.parameters.PersistableRemoteHostV4
import mqtt.client.persistence.IMqttConnectionsDb
import mqtt.client.persistence.QueuedObjectCollection

abstract class MqttRoomDatabase : RoomDatabase(), IMqttConnectionsDb


interface MqttConnectionsDatabaseDescriptor : Parcelable {
    fun getDb(context: Context): IMqttConnectionsDb
    fun getPersistence(context: Context, remoteHost: PersistableRemoteHostV4): QueuedObjectCollection

    companion object {
        val TAG = MqttConnectionsDatabaseDescriptor::class.java.canonicalName!!
    }
}

abstract class MqttDatabaseDescriptor<Database : MqttRoomDatabase>(private val type: Class<out Database>) :
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