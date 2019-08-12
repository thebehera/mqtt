@file:Suppress("EXPERIMENTAL_API_USAGE")

package mqtt.client.session

import io.ktor.http.Url
import mqtt.client.persistence.KeyValuePersistence
import mqtt.client.persistence.MemoryKeyValuePersistence
import mqtt.client.subscription.SubscriptionManager
import mqtt.wire.control.packet.ISubscribeAcknowledgement
import mqtt.wire.control.packet.ISubscribeRequest
import mqtt.wire.data.MqttUtf8String
import mqtt.wire.data.topic.SubscriptionCallback
import kotlin.reflect.KClass

class ClientSessionState(
        val qos1And2MessagesSentButNotAcked: KeyValuePersistence = MemoryKeyValuePersistence(),
        val qos2MessagesRecevedButNotCompletelyAcked: KeyValuePersistence = MemoryKeyValuePersistence()) {
    val subscriptionManager = SubscriptionManager()
    val unacknowledgedSubscriptions = HashMap<UShort, ISubscribeRequest>()

    suspend fun start(clientId: MqttUtf8String, server: Url) {
        qos1And2MessagesSentButNotAcked.open(clientId, server)
        qos2MessagesRecevedButNotCompletelyAcked.open(clientId, server)
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

    suspend fun close() {
        qos1And2MessagesSentButNotAcked.close()
        qos2MessagesRecevedButNotCompletelyAcked.close()
    }

}

