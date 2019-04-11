package mqtt.client

import mqtt.wire.data.QualityOfService
import mqtt.wire.data.topic.Name

interface SubscriptionCallback<T> {
    fun onMessageReceived(topic: Name, qos: QualityOfService, message: T)
}
