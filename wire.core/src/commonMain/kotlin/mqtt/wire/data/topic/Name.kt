@file:Suppress("EXPERIMENTAL_FEATURE_WARNING")

package mqtt.wire.data.topic

import mqtt.wire.data.MqttUtf8String

inline class Name(val topic: String) {

    fun validateTopic(): TopicLevelNode? {
        return try {
            val rootNode = TopicLevelNode.from(MqttUtf8String(topic))
            if (rootNode.hasWildcardInTopic()) {
                return null
            }
            rootNode
        } catch (e: Exception) {
            null
        }
    }


}
