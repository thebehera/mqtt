@file:Suppress("UNCHECKED_CAST")

package mqtt.client.subscription

import mqtt.wire.data.topic.Filter
import mqtt.wire.data.topic.TopicLevelNode
import mqtt.wire.data.topic.minusAssign
import mqtt.wire.data.topic.plusAssign
import kotlin.reflect.KClass

class SubscriptionManager {
    private val nodeToClassMapping = HashMap<TopicLevelNode, KClass<*>>()
    private val classToSerializerMapping = HashMap<KClass<*>, SubscriptionCallback<*>>()
    val rootNodeSubscriptions = HashMap<String, TopicLevelNode>()


    inline fun <reified T : Any> register(rootTopicNode: Filter, callback: SubscriptionCallback<T>) {
        val validated = rootTopicNode.validate() ?: return
        val rootTopicValue = validated.value.value
        val previouslyFoundRootNode = rootNodeSubscriptions[rootTopicValue]
        if (previouslyFoundRootNode == null) {
            rootNodeSubscriptions[rootTopicValue] = validated
        } else {
            previouslyFoundRootNode += validated
        }
        registerInternal(validated.getAllBottomLevelChildren().first(), callback, T::class)
    }


    fun deregister(topic: String) {
        val rootTopicNode = TopicLevelNode.parse(topic) ?: throw IllegalArgumentException("Topic is invalid: $topic")
        val oldTopicLevelNode = rootNodeSubscriptions[rootTopicNode.value.value] ?: return
        oldTopicLevelNode -= rootTopicNode
        rootTopicNode.getAllBottomLevelChildren().forEach {
            deregisterInternal(it)
        }
    }

    fun <T : Any> getSubscription(topicLevelNode: TopicLevelNode): SubscriptionCallback<T>? {
        val klass = nodeToClassMapping[topicLevelNode] ?: return null
        return classToSerializerMapping[klass] as SubscriptionCallback<T>
    }

    fun <T : Any> registerInternal(level: TopicLevelNode, callback: SubscriptionCallback<T>, genericClassRef: KClass<T>) {
        nodeToClassMapping[level] = genericClassRef
        classToSerializerMapping[genericClassRef] = callback as SubscriptionCallback<Any>
    }


    fun deregisterInternal(level: TopicLevelNode) {
        val genericClassRef = nodeToClassMapping.remove(level)
        classToSerializerMapping.remove(genericClassRef)
        rootNodeSubscriptions.remove(level.value.value)
    }
}
