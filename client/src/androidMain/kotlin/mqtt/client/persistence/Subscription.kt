package mqtt.client.persistence

import android.os.Parcelable
import androidx.room.Entity
import androidx.room.TypeConverters
import kotlinx.android.parcel.Parcelize

@Entity(primaryKeys = ["connectionIdentifier", "topicFilter"])
@TypeConverters(MqttV4TypeConverters::class)
@Parcelize
data class MqttSubscription(
    val connectionIdentifier: Int,
    val topicFilter: String,
    val kclass: String,
    val packetIdentifier: Int = 0
) : Parcelable
