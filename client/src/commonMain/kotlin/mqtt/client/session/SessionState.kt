@file:Suppress("EXPERIMENTAL_API_USAGE")

package mqtt.client.session

import mqtt.client.persistence.KeyValuePersistence
import mqtt.client.persistence.MemoryKeyValuePersistence
import mqtt.client.persistence.MemoryQueuedPersistence
import mqtt.client.persistence.QueuedPersistence
import mqtt.client.subscription.SubscriptionManager
import mqtt.wire.control.packet.ISubscribeAcknowledgement
import mqtt.wire.control.packet.ISubscribeRequest

class ClientSessionState(
        val messagesNotSent: QueuedPersistence = MemoryQueuedPersistence(),
        val qos1And2MessagesSentButNotAcked: KeyValuePersistence = MemoryKeyValuePersistence(),
        val qos2MessagesRecevedButNotCompletelyAcked: KeyValuePersistence = MemoryKeyValuePersistence()) {
    val subscriptionManager = SubscriptionManager()
    val unacknowledgedSubscriptions = HashMap<UShort, ISubscribeRequest>()

    fun sentSubscriptionRequest(msg: ISubscribeRequest) {

    }

    fun subscriptionAcknowledgementReceived(msg: ISubscribeAcknowledgement) {

    }

}

