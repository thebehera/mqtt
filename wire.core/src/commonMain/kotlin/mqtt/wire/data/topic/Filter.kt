@file:Suppress("EXPERIMENTAL_FEATURE_WARNING")

package mqtt.wire.data.topic

import mqtt.wire.data.MqttUtf8String

inline class Filter(private val topicFilter: String) {
    fun validate(asServer: Boolean = false): TopicLevelNode? {
        return try {
            val rootNode = TopicLevelNode.from(MqttUtf8String(topicFilter))
            if (!asServer && rootNode.value.value.startsWith('$')) {
                return null
            }
            rootNode
        } catch (e: Exception) {
            null
        }
    }
}
