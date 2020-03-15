@file:Suppress("EXPERIMENTAL_FEATURE_WARNING")

package mqtt.wire.data.topic

import mqtt.Parcelable
import mqtt.Parcelize
import mqtt.wire.data.MqttUtf8String

@Parcelize
data class Filter(val topicFilter: CharSequence) : Parcelable {
    fun validate(asServer: Boolean = false): Node? {
        return try {
            val rootNode = Node.from(MqttUtf8String(topicFilter))
            if (!asServer && rootNode.level.value.startsWith('$')) {
                return null
            }
            rootNode
        } catch (e: Exception) {
            null
        }
    }

}
