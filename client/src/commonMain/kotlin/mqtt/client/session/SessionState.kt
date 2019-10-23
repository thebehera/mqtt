@file:Suppress("EXPERIMENTAL_API_USAGE")

package mqtt.client.session

import mqtt.client.persistence.QueuedObjectCollection
import mqtt.client.subscription.SubscriptionManager
import mqtt.connection.IRemoteHost
import mqtt.wire.control.packet.ISubscribeAcknowledgement
import mqtt.wire.control.packet.ISubscribeRequest
import mqtt.wire.data.topic.SubscriptionCallback
import kotlin.reflect.KClass

class ClientSessionState(val queue: QueuedObjectCollection, val remoteHost: IRemoteHost) {
    val subscriptionManager = SubscriptionManager()
    val unacknowledgedSubscriptions = HashMap<Int, ISubscribeRequest>()

    suspend fun start() {
        queue.open(remoteHost)
    }

    fun <T : Any> sentSubscriptionRequest(
        msg: ISubscribeRequest,
        typeClass: KClass<T>,
        callbacks: List<SubscriptionCallback<T>>
    ) {
        unacknowledgedSubscriptions[msg.packetIdentifier] = msg
        val topics = msg.getTopics()
        topics.forEachIndexed { index, filter ->
            val node = filter.validate()
            if (node == null) {
                println("Failed to validate $filter")
            } else {
                subscriptionManager.register(node, typeClass, callbacks[index])
            }
        }
    }

    inline fun <reified T : Any> sentSubscriptionRequest(
        msg: ISubscribeRequest,
        callbacks: List<SubscriptionCallback<T>>
    ) {
        unacknowledgedSubscriptions[msg.packetIdentifier] = msg
        val topics = msg.getTopics()
        topics.forEachIndexed { index, filter ->
            val node = filter.validate()
            if (node == null) {
                println("Failed to validate $filter")
            } else {
                subscriptionManager.register(node, callbacks[index])
            }
        }
    }

    fun subscriptionAcknowledgementReceived(msg: ISubscribeAcknowledgement) {
        val subRequestFound = unacknowledgedSubscriptions.remove(msg.packetIdentifier)
        if (subRequestFound == null) {
            println("Failed to find subscription request")
            return
        }
    }

}

