@file:Suppress("EXPERIMENTAL_API_USAGE")

package mqtt.android_app.room

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.TypeConverters
import mqtt.Parcelize
import mqtt.client.persistence.QueuedDb
import mqtt.client.persistence.QueuedMessage
import mqtt.client.persistence.RoomTypeConverters
import mqtt.connection.ILogConfiguration
import mqtt.connection.IMqttConfiguration
import mqtt.connection.IRemoteHost
import mqtt.persistence.MqttPersistence

@Database(entities = [Test1::class, QueuedMessage::class], version = 1)
@TypeConverters(RoomTypeConverters::class)
abstract class ApplicationDb : QueuedDb() {
    abstract fun test1Dao(): Test1Dao
}

@Parcelize
class AndroidConfiguration(
    override val remoteHost: IRemoteHost,
    override val logConfiguration: ILogConfiguration
) : IMqttConfiguration {
    override fun persistenceLayer(): MqttPersistence<QueuedMessage> {
        return queuedDb.mqttQueued()
    }
}

lateinit var appContext: Context

fun initQueuedDb(context: Context): QueuedDb {
    if (::appContext.isInitialized) {
        return queuedDb
    }
    appContext = context.applicationContext
    return queuedDb
}

val queuedDb by lazy {
    Room.databaseBuilder(appContext, ApplicationDb::class.java, "testdb").build()
}
