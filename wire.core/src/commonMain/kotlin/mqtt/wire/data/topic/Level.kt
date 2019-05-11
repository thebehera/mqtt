package mqtt.wire.data.topic

import mqtt.wire.ProtocolError

sealed class TopicLevel(val value: String) {
    override fun toString() = value
}

sealed class LevelWildcard(valueL: String) : TopicLevel(valueL)

object MultiLevelWildcard : LevelWildcard("#")
object SingleLevelWildcard : LevelWildcard("+")
object EmptyValue : TopicLevel("")
object RootTopicValue : TopicLevel(Node.ROOT_TOPIC_NODE_VALUE)
data class StringTopicLevel(val topicValue: String) : TopicLevel(topicValue) {
    init {
        if (topicValue.contains('#')) throw ProtocolError("Invalid character # in topic level $topicValue")
        if (topicValue.contains('+')) throw ProtocolError("Invalid character + in topic level $topicValue")
    }
}

fun String.toTopicLevel(): TopicLevel {
    return when (this) {
        "#" -> MultiLevelWildcard
        "+" -> SingleLevelWildcard
        "" -> EmptyValue
        Node.ROOT_TOPIC_NODE_VALUE -> RootTopicValue
        else -> StringTopicLevel(this)
    }
}