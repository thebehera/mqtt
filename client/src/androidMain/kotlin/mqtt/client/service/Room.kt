@file:Suppress("EXPERIMENTAL_API_USAGE")

package mqtt.client.service

import android.content.Context
import android.os.Parcelable
import androidx.room.Room
import androidx.room.RoomDatabase
import mqtt.client.MqttClient
import mqtt.client.persistence.IMqttConnectionsDb
import mqtt.client.persistence.QueuedObjectCollection
import mqtt.wire.data.QualityOfService
import mqtt.wire.data.topic.Name
import kotlin.coroutines.CoroutineContext
import kotlin.reflect.KClass

abstract class MqttRoomDatabase : RoomDatabase(), IMqttConnectionsDb

interface MqttConnectionsDatabaseDescriptor : Parcelable {
    fun getDb(context: Context): MqttRoomDatabase
    fun getPersistence(
        context: Context,
        coroutineContext: CoroutineContext,
        connectionIdentifier: Int
    ): QueuedObjectCollection

    suspend fun <T : Any> subscribe(
        client: MqttClient, packetId: UShort, topicOverride: String?,
        qosOverride: QualityOfService?, klass: KClass<T>,
        cb: (topic: Name, qos: QualityOfService, message: T?) -> Unit
    )

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
