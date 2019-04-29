@file:Suppress("EXPERIMENTAL_API_USAGE")

package mqtt.client.session

import io.ktor.http.Url
import mqtt.client.persistence.KeyValuePersistence
import mqtt.client.persistence.MemoryKeyValuePersistence
import mqtt.client.persistence.MemoryQueuedPersistence
import mqtt.client.persistence.QueuedPersistence
import mqtt.client.subscription.SubscriptionCallback
import mqtt.client.subscription.SubscriptionManager
import mqtt.wire.control.packet.ISubscribeAcknowledgement
import mqtt.wire.control.packet.ISubscribeRequest
import mqtt.wire.data.MqttUtf8String

class ClientSessionState(
        val messagesNotSent: QueuedPersistence = MemoryQueuedPersistence(),
        val qos1And2MessagesSentButNotAcked: KeyValuePersistence = MemoryKeyValuePersistence(),
        val qos2MessagesRecevedButNotCompletelyAcked: KeyValuePersistence = MemoryKeyValuePersistence()) {
    val subscriptionManager = SubscriptionManager()
    val unacknowledgedSubscriptions = HashMap<UShort, ISubscribeRequest>()

    suspend fun start(clientId: MqttUtf8String, server: Url) {
        qos1And2MessagesSentButNotAcked.open(clientId, server)
        qos2MessagesRecevedButNotCompletelyAcked.open(clientId, server)
    }

    fun sentSubscriptionRequest(msg: ISubscribeRequest, callbacks: List<SubscriptionCallback<Any>>) {
        unacknowledgedSubscriptions[msg.packetIdentifier] = msg
        val topics = msg.getTopics()
        topics.forEachIndexed { index, filter ->
            subscriptionManager.register(filter, callbacks[index])
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

