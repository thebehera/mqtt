package mqtt.wire.data.topic

import mqtt.wire.control.packet.IPublishMessage
import mqtt.wire.data.MqttUtf8String
import mqtt.wire.data.QualityOfService

data class Node internal constructor(val level: TopicLevel) {
    internal val children = mutableMapOf<TopicLevel, Node>()
    private var parent: Node? = null
    private val isRoot = level.value == ROOT_TOPIC_NODE_VALUE
    val isWildcard = level is LevelWildcard
    private val callbacks = ArrayList<CallbackTypeReference<*>>()

    fun <T : Any> addCallback(callback: CallbackTypeReference<T>) {
        callbacks += callback
    }

    fun <T : Any> removeCallback(callback: CallbackTypeReference<T>) {
        callbacks -= callback
    }

    fun handlePublish(msg: IPublishMessage) {
        callbacks.forEach { it.handleCallback(msg) }
    }

    override fun toString(): String {
        if (isRoot) {
            return "ROOT NODE W/CHILDREN: ${allChildren()}"
        }
        val parent = parent!! // if we are not root we have a parent root
        return if (parent.isRoot) {
            level.value
        } else {
            (parent.toString()) + TOPIC_SEPERATOR + level.value
        }
    }

    fun addChild(node: Node) {
        children[node.level] = node
        node.parent = this
    }

    fun find(otherTopic: Name): Node? {
        var currentLocalNode = root()
        otherTopic.validate() ?: throw IllegalArgumentException("$otherTopic is an invalid topic name")
        // even though we have a topic node, there is no way to have multiple nodes for the validatedOtherTopic
        val otherTopicNameSplit = otherTopic.topic.split(TOPIC_SEPERATOR)
        for (otherLevel in otherTopicNameSplit) {
            if (currentLocalNode.level is MultiLevelWildcard) {
                return currentLocalNode
            }
            val childNodeMatch = currentLocalNode.children[otherLevel.toTopicLevel()]
                    ?: currentLocalNode.children[MultiLevelWildcard]
                    ?: currentLocalNode.children[SingleLevelWildcard]
                    ?: return null
            currentLocalNode = childNodeMatch
        }
        return currentLocalNode
    }


    fun allChildren(): Set<Node> {
        val childrenLocal = mutableSetOf<Node>()
        for (child in children.values) {
            childrenLocal += child.allChildren()
        }
        if (children.isEmpty()) {
            childrenLocal += this
        }
        return childrenLocal
    }

    fun matchesTopic(otherTopic: Node) = find(Name(otherTopic.toString())) != null

    fun root(): Node = parent?.root() ?: this

    fun detachFromParent() {
        parent?.children?.remove(level)
        parent = null
    }

    companion object {
        private const val TOPIC_SEPERATOR = '/'
        internal const val ROOT_TOPIC_NODE_VALUE = "\$SYS_ROOT_TOPIC_DEFAULT"

        fun from(topic: MqttUtf8String) = parse(topic.getValueOrThrow())

        fun parse(topic: String): Node {
            val rootNode = Node(RootTopicValue)
            if (topic.isEmpty()) {
                val emptyNode = Node(EmptyValue)
                rootNode.addChild(emptyNode)
                return emptyNode
            }
            var currentNode = rootNode
            val topicsSplit = topic.split(TOPIC_SEPERATOR)
            for (topicLevelString in topicsSplit) {
                val child = Node(topicLevelString.toTopicLevel())
                currentNode.addChild(child)
                currentNode = child
            }
            return currentNode
        }

        fun newRootNode() = Node(ROOT_TOPIC_NODE_VALUE.toTopicLevel())
    }
}

operator fun Node.plusAssign(other: Node) {
    for ((topicLevel, newChildNode) in other.children) {
        val originalChild = children[topicLevel]
        if (originalChild != null) {
            originalChild += newChildNode
            continue
        }
        addChild(newChildNode)
    }
}

operator fun Node.minusAssign(other: Node) {
    val foundChildNode = find(Name(other.toString()))
    foundChildNode?.detachFromParent()
}

interface SubscriptionCallback<T> {
    fun onMessageReceived(topic: Name, qos: QualityOfService, message: T?)
}
