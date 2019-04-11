package mqtt.wire.data.topic

sealed class TopicLevel(val value: String) {
    override fun toString() = value
}


object MultiLevelWildcard : TopicLevel("#")
object SingleLevelWildcard : TopicLevel(("+"))
object EmptyValue : TopicLevel("")
data class StringTopicLevel(val topicValue: String) : TopicLevel(topicValue)
