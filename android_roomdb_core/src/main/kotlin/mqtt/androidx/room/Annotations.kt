package mqtt.androidx.room

import androidx.room.Database
import mqtt.androidx.room.QualityOfService.AT_LEAST_ONCE
import kotlin.annotation.AnnotationRetention.SOURCE
import kotlin.annotation.AnnotationTarget.CLASS
import kotlin.annotation.AnnotationTarget.FUNCTION

@Retention(SOURCE)
annotation class MqttDatabase(val db: Database)

@Retention(SOURCE)
@Target(CLASS)
annotation class MqttPublish(
    val defaultTopic: String = "",
    val defaultQos: QualityOfService = AT_LEAST_ONCE,
    val defaultDup: Boolean = false,
    val defaultRetain: Boolean = false
) //TODO: How do we serialize/upload to the network?

// Deque from stored message collection so the mqtt process can send it off incase of network/power failure
@Retention(SOURCE)
@Target(FUNCTION)
annotation class MqttPublishDequeue

@Retention(SOURCE)
annotation class MqttPublishSize

@Retention(SOURCE)
@Target(FUNCTION)
annotation class MqttPublishPacket

@Retention(SOURCE)
annotation class MqttSubscribe(
    val defaultTopicFilter: String,
    val defaultQos: QualityOfService = AT_LEAST_ONCE
)

enum class QualityOfService {
    AT_MOST_ONCE,
    AT_LEAST_ONCE,
    EXACTLY_ONCE
}