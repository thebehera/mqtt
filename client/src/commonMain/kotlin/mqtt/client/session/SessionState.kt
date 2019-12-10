@file:Suppress("EXPERIMENTAL_API_USAGE")

package mqtt.client.session

import mqtt.client.persistence.QueuedObjectCollection
import mqtt.connection.IRemoteHost
import mqtt.wire.control.packet.ISubscribeAcknowledgement
import mqtt.wire.control.packet.ISubscribeRequest
import mqtt.wire.data.topic.SubscriptionCallback
import kotlin.reflect.KClass

class ClientSessionState(val queue: QueuedObjectCollection, val remoteHost: IRemoteHost) {
    val subscriptionManager = SubscriptionManager()

    // TODO: Use the QueuedObjectCollection for proper persistence
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
        println("send subscription request register ${msg.packetIdentifier}")
        val topics = msg.getTopics()
        topics.forEachIndexed { index, filter ->
            val node = filter.validate()
            if (node == null) {
                println("Failed to validate $filter")
            } else {
                subscriptionManager.register(node, typeClass, callbacks[index])
            }
        }
        println("Sent sub request")
    }

    inline fun <reified T : Any> sentSubscriptionRequest(
        msg: ISubscribeRequest,
        callbacks: List<SubscriptionCallback<T>>
    ) {
        unacknowledgedSubscriptions[msg.packetIdentifier] = msg
        println("send subscription request register ${msg.packetIdentifier}")
        val topics = msg.getTopics()
        topics.forEachIndexed { index, filter ->
            val node = filter.validate()
            if (node == null) {
                println("Failed to validate $filter")
            } else {
                subscriptionManager.register(node, callbacks[index])
            }
        }
        println("Sent sub request")
    }

    fun subscriptionAcknowledgementReceived(msg: ISubscribeAcknowledgement) {
        val subRequestFound = unacknowledgedSubscriptions.remove(msg.packetIdentifier)
        println("got suback ${msg.packetIdentifier}")
        if (subRequestFound == null) {
            println("Failed to find subscription request")
            return
        }
    }

}

