@file:Suppress("EXPERIMENTAL_API_USAGE")

package mqtt.wire.data.topic

import mqtt.buffer.*
import mqtt.wire.control.packet.IPublishMessage
import mqtt.wire.data.MqttUtf8String
import mqtt.wire.data.QualityOfService

data class Node internal constructor(val level: TopicLevel) {
    val children = mutableMapOf<TopicLevel, Node>()
    private var parent: Node? = null
    private val isRoot = level.value == ROOT_TOPIC_NODE_VALUE
    val isWildcard = level is LevelWildcard
    private val callbacks = ArrayList<CallbackTypeReference<*>>()
    val deserializers = HashSet<BufferDeserializer<*>>()
    val serializers = HashSet<BufferSerializer<Any>>()

    fun <T : Any> addCallback(callback: CallbackTypeReference<T>) {
        callbacks += callback
    }

    fun <T : Any> removeCallback(callback: CallbackTypeReference<T>) {
        callbacks -= callback
    }

    fun handlePublish(msg: IPublishMessage) {
        callbacks.forEach { it.handleCallback(msg) }
    }

    fun <T : Any> registerPublishDeserializer(deserializer: BufferDeserializer<T>) {
        deserializers.add(deserializer)

    }

    fun registerPublishSerializer(serializer: BufferSerializer<Any>) {
        serializers.add(serializer)
    }

    fun deserializePublish(
        buffer: ReadBuffer,
        length: UShort,
        headers: Map<CharSequence, Set<CharSequence>> = emptyMap()
    ): GenericType<out Any>? {
        val topic = toString()
        deserializers.forEach {
            val deserialized = it.deserialize(buffer, length, topic, headers)
            if (deserialized != null) {
                return deserialized
            }
        }
        return null
    }

    fun serializePublish(buffer: WriteBuffer, obj: GenericType<Any>) {
        serializers.forEach {
            if (it.serialize(buffer, obj.obj)) {
                return@forEach
            }
        }
    }

    override fun toString(): String {
        if (isRoot) {
            return "ROOT NODE W/CHILDREN: ${allChildren()}"
        }
        val parent = parent!! // if we are not root we have a parent root
        return if (parent.isRoot) {
            level.value.toString()
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

        fun parse(topic: CharSequence): Node {
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

fun Node.addName(other: Name): Node {
    val node = Node(other.topic.toTopicLevel())
    this += node
    return node
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

interface SubscriptionCallback<in T> {
    fun onMessageReceived(topic: Name, qos: QualityOfService, message: T?)
}
