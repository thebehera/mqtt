@file:Suppress("EXPERIMENTAL_API_USAGE")

package mqtt.client.persistence

import androidx.room.RoomDatabase

abstract class MqttRoomDatabase : RoomDatabase(), IMqttConnectionsDb

