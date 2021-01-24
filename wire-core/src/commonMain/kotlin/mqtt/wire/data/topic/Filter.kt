@file:Suppress("EXPERIMENTAL_FEATURE_WARNING")

package mqtt.wire.data.topic

data class Filter(val topicFilter: CharSequence) {
    fun validate(asServer: Boolean = false): Node? {
        return try {
            val rootNode = Node.from(topicFilter)
            if (!asServer && rootNode.level.value.startsWith('$')) {
                return null
            }
            rootNode
        } catch (e: Exception) {
            null
        }
    }

}
