package mqtt.wire.data.topic

import mqtt.wire.data.MqttUtf8String

/**
 * Represent the topics in a tree like structure. Saves memory for multiple topics with a similar structure compared
 * to using a string and speeds up lookups to see if a topic matches a structure
 */
data class TopicLevelNode(val value: TopicLevel) {
    val children: MutableMap<String, TopicLevelNode> = mutableMapOf()
    var parent: TopicLevelNode? = null
    /**
     * Is this topic level a wildcard
     */
    val isWildcard = value is MultiLevelWildcard || value is SingleLevelWildcard

    /**
     * Check if this topic or any children has a wildcard
     */
    fun hasWildcardInTopic(): Boolean {
        if (children.isEmpty()) {
            return isWildcard
        }
        children.forEach {
            if (it.value.hasWildcardInTopic()) {
                return true
            }
        }
        return isWildcard
    }

    fun detachFromParent() {
        val parent = parent
        parent?.children?.remove(value.value)
        this.parent = null
    }

    fun findSimilarChildNode(otherChildNode: TopicLevelNode): TopicLevelNode? {
        var currentChildOtherNode: TopicLevelNode? = otherChildNode.parent
        val keyPath = ArrayList<TopicLevel>()
        keyPath.add(otherChildNode.value)
        while (currentChildOtherNode != null) {
            keyPath.add(currentChildOtherNode.value)
            currentChildOtherNode = currentChildOtherNode.parent
        }
        keyPath.reverse()
        var index = 1
        val keyPathCurrent = keyPath.getOrNull(index++)?.value
        var currentChildThisNode = children[keyPathCurrent]

        while (index < keyPath.size) {
            val nextKey = keyPath.getOrNull(index++)?.value!!
            if (currentChildThisNode == null) {
                return null
            } else {
                currentChildThisNode = currentChildThisNode.children[nextKey]
            }
        }
        return currentChildThisNode
    }

    fun getAllBottomLevelChildren(): Set<TopicLevelNode> {
        if (children.isEmpty()) {
            return setOf(this)
        }
        val childrensChildren = mutableSetOf<TopicLevelNode>()
        children.values.forEach {
            childrensChildren += it.getAllBottomLevelChildren()
        }
        return childrensChildren
    }

    fun getCurrentPath(): String {
        val parent = parent
        val parentPath = parent?.getCurrentPath()
        return if (parentPath == null) {
            value.value
        } else {
            parentPath + "/" + value.value
        }
    }

    override fun toString() = getCurrentPath()

    /**
     * Check and see if the other topic matches this topic structure.
     */
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
            if (missingKeys.contains("+") || missingKeys.contains("#")) {

            } else if (missingKeys.isNotEmpty()) {
                return false
            } else {
                other.children.forEach { otherChildPair ->
                    val otherKey = otherChildPair.key
                    val otherValue = otherChildPair.value
                    val matchingChild = children[otherKey] ?: return false
                    if (!matchingChild.matchesTopic(otherValue)) {
                        return false
                    }
                }
            }
        }
        val isEitherAWildcard = isWildcard || other.isWildcard
        if (!isEitherAWildcard) {
            if (value != other.value) {
                return false
            }
        }

        if (children.size != other.children.size && children["#"] == null && !isEitherAWildcard) {
            return false
        }

        other.children.forEach { otherChildPair ->
            val otherKey = otherChildPair.key
            if (otherKey != "+") {
                val otherValue = otherChildPair.value
                var matchingChild = children[otherKey]
                if (matchingChild == null) {
                    matchingChild = children["+"]
                }
                if (matchingChild == null) {
                    matchingChild = children["#"]
                }
                val child = matchingChild
                        ?: return false
                if (!child.matchesTopic(otherValue)) {
                    return false
                }
            }
        }
        if (other.children.isEmpty() && children.isNotEmpty() && (other.value == SingleLevelWildcard || value == SingleLevelWildcard)) {
            return false
        }
        return true
    }

    companion object {
        private const val TOPIC_LEVEL_SEPERATOR = '/'
        /**
         * Convert an MQTT UTF8 Compliant string into a topic level node structure
         */
        fun from(topic: MqttUtf8String): TopicLevelNode {
            val topicValidated = topic.getValueOrThrow()
            if (topicValidated.isEmpty()) {
                return TopicLevelNode(EmptyValue)
            }
            return parse(topicValidated) ?: TopicLevelNode(EmptyValue)
        }

        /**
         * Convert the string format of a suspected topic into a topic level tree structure returning the root node
         */
        fun parse(topic: String, parent: TopicLevelNode? = null): TopicLevelNode? {
            if (parent == null && topic.startsWith('/')) {
                val topTopicLevel = EmptyValue
                val child = parse(topic.substring(1), parent)
                val node = TopicLevelNode(topTopicLevel)
                if (child != null) {
                    child.parent = node
                    node.children[child.value.value] = child
                }
                return node
            }
            if (topic.endsWith("/")) {
                val parsedWithoutSuffix = parse(topic.substringBeforeLast("/"), parent)
                val bottomLevelNode = parsedWithoutSuffix?.getAllBottomLevelChildren()?.first() ?: return null
                val emptyNode = TopicLevelNode(EmptyValue)
                emptyNode.parent = bottomLevelNode
                bottomLevelNode.children[""] = emptyNode
                return parsedWithoutSuffix

            }
            val topics = topic.split(TOPIC_LEVEL_SEPERATOR, limit = 2)
            if (topics.isEmpty()) {
                return null
            }
            val currentTopicLevel = topics[0]
            if (currentTopicLevel.isEmpty()) {
                throw IllegalArgumentException("Invalid size of topic level")
            }
            if (currentTopicLevel.length > 1) {
                val multiLevelWildcardIndex = currentTopicLevel.indexOf('#')
                if (multiLevelWildcardIndex > 0) {
                    throw IllegalArgumentException("Error building topic ($currentTopicLevel): The multi-level " +
                            "wildcard character MUST be specified either on its own or following a topic level " +
                            "separator. In either case it MUST be the last character specified in the Topic Filter" +
                            " [MQTT-4.7.1-1]")
                }
                val singleLevelWildcardIndex = currentTopicLevel.indexOf('+')
                if (singleLevelWildcardIndex > 0) {
                    throw IllegalArgumentException("Error building topic ($currentTopicLevel): The single-level " +
                            "wildcard MUST occupy an entire level of the filter [MQTT-4.7.1-2].")
                }
            }
            when {
                topics.size == 1 -> {
                    if (currentTopicLevel == "#") {
                        return TopicLevelNode(MultiLevelWildcard)
                    }
                    if (currentTopicLevel == "+") {
                        return TopicLevelNode(SingleLevelWildcard)
                    }
                    if (currentTopicLevel.isBlank()) {
                        return null
                    }
                    val node = TopicLevelNode(StringTopicLevel(currentTopicLevel))
                    node.parent = parent
                    return node
                }
                topics.size == 2 -> {
                    if (currentTopicLevel == "#") {
                        throw IllegalArgumentException("Invalid multilevel wildcard found at position ${currentTopicLevel.length} in topic: $topic. Was not expecting any more characters after that.")
                    }
                    val restOfTopic = topics[1]
                    val level = when (currentTopicLevel) {
                        "#" -> MultiLevelWildcard
                        "+" -> SingleLevelWildcard
                        else -> StringTopicLevel(currentTopicLevel)
                    }
                    if (restOfTopic.isEmpty()) {
                        return TopicLevelNode(level)
                    }

                    val childTopicLevel = parse(topics[1])
                    val result = if (childTopicLevel != null) {
                        Pair(childTopicLevel.value.value, childTopicLevel)
                    } else {
                        null
                    }
                    val currentNode = TopicLevelNode(level)
                    if (result != null) {
                        currentNode.children[result.first] = result.second
                    }
                    childTopicLevel?.parent = currentNode
                    currentNode.parent = parent
                    return currentNode
                }
                else -> throw RuntimeException(
                        "Topic split operation failing. Expected a max of two string splits, got more")
            }
        }
    }
}

/**
 * Add the topics in to the tree structure of the originial 'self' topic level node
 */
fun addTopicLevelNode(self: TopicLevelNode, other: TopicLevelNode) {
    if (self.value == other.value) {
        val missingChildren = other.children.keys - self.children.keys
        missingChildren.forEach {
            val missingChild = other.children[it]!!
            self.children[missingChild.value.value] = missingChild
            missingChild.parent = self
        }
        val duplicateChildrenKeys = self.children.keys.intersect(other.children.keys)
        duplicateChildrenKeys.forEach {
            addTopicLevelNode(self.children[it]!!, other.children[it]!!)
        }
    }
}

/**
 * Merge the two topics together into one structure
 */
operator fun TopicLevelNode.plusAssign(other: TopicLevelNode) {
    addTopicLevelNode(this, other)
}


operator fun TopicLevelNode.minusAssign(other: TopicLevelNode) {
    removeTopicLevelNode(this, other)
}

fun removeTopicLevelNode(self: TopicLevelNode, other: TopicLevelNode) {
    val otherChildren = other.getAllBottomLevelChildren()
    otherChildren.forEach {
        val node = self.findSimilarChildNode(it)
        if (node != null) {
            node.detachFromParent()
        }
    }
}
