package mqtt.client

import mqtt.client.persistence.KeyValuePersistence
import mqtt.client.persistence.MemoryKeyValuePersistence
import mqtt.client.persistence.MemoryQueuedPersistence
import mqtt.client.persistence.QueuedPersistence

class ClientSessionState(
        val messagesNotSent: QueuedPersistence = MemoryQueuedPersistence(),
        val qos1And2MessagesSentButNotAcked: KeyValuePersistence = MemoryKeyValuePersistence(),
        val qos2MessagesRecevedButNotCompletelyAcked: KeyValuePersistence = MemoryKeyValuePersistence())

