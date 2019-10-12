package mqtt.client.service

import android.content.Context
import android.os.Parcelable
import mqtt.client.connection.parameters.RemoteHostDao

interface IMqttConnectionsDb {
    fun remoteHostsDao(): RemoteHostDao
}

interface MqttConnectionsDatabaseDescriptor : Parcelable {
    fun getDb(context: Context): IMqttConnectionsDb

    companion object {
        val TAG = MqttConnectionsDatabaseDescriptor::class.java.canonicalName!!
    }
}
