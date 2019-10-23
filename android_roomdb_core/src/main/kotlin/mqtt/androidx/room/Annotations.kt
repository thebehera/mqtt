package mqtt.androidx.room

import androidx.room.Database
import mqtt.androidx.room.QualityOfService.AT_LEAST_ONCE

annotation class MqttDatabase(val db: Database)

annotation class MqttPublish(
    val defaultTopic: String = "",
    val defaultQos: QualityOfService = AT_LEAST_ONCE,
    val defaultDup: Boolean = false,
    val defaultRetain: Boolean = false
) //TODO: How do we serialize/upload to the network?

annotation class MqttPublishSize
annotation class MqttPublishPacket

enum class QualityOfService {
    AT_MOST_ONCE,
    AT_LEAST_ONCE,
    EXACTLY_ONCE
}