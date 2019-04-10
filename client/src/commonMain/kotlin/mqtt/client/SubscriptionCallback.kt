package mqtt.client

import mqtt.wire.data.QualityOfService

interface SubscriptionCallback<T> {
    val topic: String
    val qos: QualityOfService
    fun onMessageReceived(message: T)
}
