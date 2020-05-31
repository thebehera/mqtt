package mqtt.client.persistence

import androidx.room.TypeConverter
import mqtt.wire.control.packet.format.fixed.DirectionOfFlow
import mqtt.wire.data.MqttUtf8String
import mqtt.wire.data.QualityOfService
import java.util.*

class MqttV4TypeConverters {
    @TypeConverter
    fun fromDirection(direction: DirectionOfFlow): String = direction.name

    @TypeConverter
    fun toDirection(direction: String): DirectionOfFlow = DirectionOfFlow.valueOf(direction)

    @TypeConverter
    fun fromQos(qos: QualityOfService): String = qos.name

    @TypeConverter
    fun toQos(qos: String): QualityOfService = QualityOfService.valueOf(qos)

    @TypeConverter
    fun fromMqttUtf8String(mqttString: MqttUtf8String?): String? = mqttString?.value?.toString()

    @TypeConverter
    fun toMqttUtf8String(mqttString: String?): MqttUtf8String? =
        if (mqttString == null) null else MqttUtf8String(mqttString)

    @TypeConverter
    fun fromDate(date: Date): Long = date.time

    @TypeConverter
    fun toDate(time: Long): Date = Date(time)

}