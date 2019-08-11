@file:Suppress("UNCHECKED_CAST")

package mqtt.client.subscription

import mqtt.wire.control.packet.IPublishMessage
import mqtt.wire.data.topic.CallbackTypeReference
import mqtt.wire.data.topic.Node
import mqtt.wire.data.topic.SubscriptionCallback
import mqtt.wire.data.topic.plusAssign
import kotlin.reflect.KClass

class SubscriptionManager {
    val registeredSubscriptionsRootNode = Node.newRootNode()

    inline fun <reified T : Any> register(level: Node, cb: SubscriptionCallback<T>) {
        val callbackTypeReference = CallbackTypeReference(cb, T::class)
        level.addCallback(callbackTypeReference)
        val root = level.root()
        registeredSubscriptionsRootNode += root
    }

    fun <T : Any> register(level: Node, typeClass: KClass<T>, cb: SubscriptionCallback<T>) {
        val callbackTypeReference = CallbackTypeReference(cb, typeClass)
        level.addCallback(callbackTypeReference)
        val root = level.root()
        registeredSubscriptionsRootNode += root
    }

    fun handleIncomingPublish(publish: IPublishMessage) {
        val topicLevelFound = registeredSubscriptionsRootNode.find(publish.topic)
        topicLevelFound?.handlePublish(publish)
    }
}
