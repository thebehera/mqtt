@file:Suppress("EXPERIMENTAL_API_USAGE")

package mqtt.client.persistence

import androidx.room.TypeConverter

class RoomTypeConverters {
    @TypeConverter
    fun fromUShort(uShort: UShort) = uShort.toInt()

    @TypeConverter
    fun intToUShort(int: Int) = int.toUShort()
}
