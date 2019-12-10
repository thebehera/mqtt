package mqtt.client.session

import mqtt.wire.control.packet.IPublishMessage
import mqtt.wire.control.packet.findSerializer
import mqtt.wire.data.topic.*
import kotlin.reflect.KClass

class SubscriptionManager {
    val registeredSubscriptionsRootNode = Node.newRootNode()

    inline fun <reified T : Any> register(level: Node, cb: SubscriptionCallback<T>) {
        val callbackTypeReference = CallbackTypeReference(cb, T::class, findSerializer())
        level.addCallback(callbackTypeReference)
        val root = level.root()
        registeredSubscriptionsRootNode += root
    }

    fun <T : Any> register(level: Node, typeClass: KClass<T>, cb: SubscriptionCallback<T>) {
        val callbackTypeReference = CallbackTypeReference(cb, typeClass, findSerializer(typeClass))
        level.addCallback(callbackTypeReference)
        val root = level.root()
        registeredSubscriptionsRootNode += root
    }

    fun unregister(level: Node) {
        registeredSubscriptionsRootNode -= level
    }

    fun handleIncomingPublish(publish: IPublishMessage): Boolean {
        val topicLevelFound = registeredSubscriptionsRootNode.find(publish.topic)
        println("incoming ($topicLevelFound) $publish")
        topicLevelFound?.handlePublish(publish)
        return true
    }
}
