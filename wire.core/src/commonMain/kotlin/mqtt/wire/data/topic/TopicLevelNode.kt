package mqtt.wire.data.topic

import mqtt.wire.data.MqttUtf8String

// Represent the topic levels as a tree structure
data class TopicLevelNode(val value: TopicLevel,
                          val children: MutableMap<String, TopicLevelNode> = mutableMapOf()) {
    val isWildcard = value is MultiLevelWildcard || value is SingleLevelWildcard

    fun hasWildcardInTopic(): Boolean {
        if (children.isEmpty()) {
            return isWildcard
        }
        children.forEach {
            if (it.value.hasWildcardInTopic()) {
                return true
            }
        }
        return false
    }

    fun doesChildrenHaveChildren(): Boolean {
        children.forEach {
            if (it.value.children.isNotEmpty()) {
                return true
            }
        }
        return false
    }

    fun doesDirectChildHaveWildcard(): Boolean {
        children.forEach {
            if (it.value.isWildcard) {
                return true
            }
        }
        return false
    }

    fun matchesTopic(other: TopicLevelNode): Boolean {
        val isCurrentTopicMultiLevelWildcard = value is MultiLevelWildcard
        val isOtherTopicMultiLevelWildcard = other.value is MultiLevelWildcard
        if (isCurrentTopicMultiLevelWildcard || isOtherTopicMultiLevelWildcard) {
            return true
        }

        val isCurrentTopicSingleLevelWildcard = value is SingleLevelWildcard
        val isOtherTopicSingleLevelWildcard = other.value is SingleLevelWildcard
        if (isCurrentTopicSingleLevelWildcard || isOtherTopicSingleLevelWildcard) {
            if (children.isEmpty() && other.children.isEmpty()) {
                return true
            }
        }

        if ((isCurrentTopicSingleLevelWildcard && children.isNotEmpty()) ||
                (isOtherTopicSingleLevelWildcard && other.children.isNotEmpty())) {
            val missingKeys = other.children.keys - children.keys
            if (missingKeys.isNotEmpty()) {
                return false
            }
            other.children.forEach { otherChildPair ->
                val otherKey = otherChildPair.key
                val otherValue = otherChildPair.value
                val matchingChild = children[otherKey] ?: return false
                if (!matchingChild.matchesTopic(otherValue)) {
                    return false
                }
            }
        }
        val isEitherAWildcard = isWildcard || other.isWildcard
        if (!isEitherAWildcard) {
            if (value != other.value) {
                return false
            }
        }

        other.children.forEach { otherChildPair ->
            val otherKey = otherChildPair.key
            val otherValue = otherChildPair.value
            var matchingChild = children[otherKey]
            if (matchingChild == null) {
                matchingChild = children["+"]
            }
            if (matchingChild == null) {
                matchingChild = children["#"]
            }
            val child = matchingChild ?: return false
            if (!child.matchesTopic(otherValue)) {
                return false
            }
        }
        return true
    }

    companion object {
        private const val TOPIC_LEVEL_SEPERATOR = '/'
        fun from(topic: MqttUtf8String): TopicLevelNode {
            val topicValidated = topic.getValueOrThrow()
            if (topicValidated.isEmpty()) {
                return TopicLevelNode(EmptyValue)
            }
            return parse(topicValidated) ?: TopicLevelNode(EmptyValue)
        }

        fun parse(topic: String): TopicLevelNode? {
            val topics = topic.split(TOPIC_LEVEL_SEPERATOR, limit = 2)
            if (topics.isEmpty()) {
                return null
            }
            val topTopicLevel = topics[0]
            when {
                topics.size == 1 -> {
                    if (topTopicLevel == "#") {
                        return TopicLevelNode(MultiLevelWildcard)
                    }
                    if (topTopicLevel == "+") {
                        return TopicLevelNode(SingleLevelWildcard)
                    }
                    if (topTopicLevel.isBlank()) {
                        return null
                    }
                    return TopicLevelNode(StringTopicLevel(topTopicLevel))
                }
                topics.size == 2 -> {
                    if (topTopicLevel == "#") {
                        throw IllegalArgumentException("Invalid multilevel wildcard found at position ${topTopicLevel.length} in topic: $topic. Was not expecting any more characters after that.")
                    }
                    if (topTopicLevel.isBlank()) {
                        throw IllegalArgumentException("Invalid topic, multiple topic level separators found")
                    }
                    val restOfTopic = topics[1]
                    val level = when (topTopicLevel) {
                        "#" -> MultiLevelWildcard
                        "+" -> SingleLevelWildcard
                        else -> StringTopicLevel(topTopicLevel)
                    }
                    if (restOfTopic.isEmpty()) {
                        return TopicLevelNode(level)
                    }

                    val childTopicNode = parse(topics[1])
                    val map = if (childTopicNode != null) {
                        mutableMapOf(Pair(childTopicNode.value.value, childTopicNode))
                    } else {
                        mutableMapOf()
                    }

                    return TopicLevelNode(level, map)
                }
                else -> throw RuntimeException(
                        "Topic split operation failing. Expected a max of two string splits, got more")
            }
        }
    }
}

fun copyFrom(self: TopicLevelNode, other: TopicLevelNode) {
    if (self.value == other.value) {
        val missingChildren = other.children.keys - self.children.keys
        missingChildren.forEach {
            val missingChild = other.children[it]!!
            self.children[missingChild.value.value] = missingChild
        }
        val duplicateChildrenKeys = self.children.keys.intersect(other.children.keys)
        duplicateChildrenKeys.forEach {
            copyFrom(self.children[it]!!, other.children[it]!!)
        }
    }
}

operator fun TopicLevelNode.plus(other: TopicLevelNode): TopicLevelNode {
    copyFrom(this, other)
    return this
}

fun main() {
    val parsed = TopicLevelNode.from(MqttUtf8String("sport/tennis/player1"))
    val parsed2 = TopicLevelNode.from(MqttUtf8String("sport/tennis/player1/#"))
    val parsed3 = TopicLevelNode.from(MqttUtf8String("sport/tennis/player1/+"))
    parsed.toString()
    parsed2.toString()
    parsed3.toString()
}

sealed class TopicLevel(val value: String) {
    override fun toString() = value
}

object MultiLevelWildcard : TopicLevel("#")
object SingleLevelWildcard : TopicLevel(("+"))
object EmptyValue : TopicLevel("")
data class StringTopicLevel(val topicValue: String) : TopicLevel(topicValue)
