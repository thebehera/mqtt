package mqtt.android_app

import android.app.Application
import mqtt.androidx.room.MqttGeneratedCodeException
import mqtt.client.persistence.MqttQueue
import mqtt.client.service.ipc.AbstractMqttServiceViewModel
import mqtt.client.service.ipc.ClientToServiceConnection
import mqtt.wire.control.packet.IPublishMessage
import mqtt.wire.data.QualityOfService
import mqtt.wire.data.topic.Name
import mqtt.wire.data.topic.SubscriptionCallback


class MqttServiceViewModelGenerated(private val app: Application) : AbstractMqttServiceViewModel(app, MqttDbProvider) {

    suspend inline fun <reified T : Any> subscribe(
        connectionIdentifier: Int, topicOverride: String? = null,
        qosOverride: QualityOfService? = null,
        crossinline cb: (topic: Name, qos: QualityOfService, message: T?) -> Unit
    ) {
        if (T::class == SimpleModel::class) {
            val topicName = topicOverride ?: "simple"
            val qos = qosOverride ?: QualityOfService.AT_LEAST_ONCE  // injected value
            subscribe(topicName, qos, connectionIdentifier, object : SubscriptionCallback<T> {
                override fun onMessageReceived(topic: Name, qos: QualityOfService, message: T?) {
                    cb(topic, qos, message)
                }
            })
        }
    }

    suspend fun <T : Any> publish(
        connectionIdentifier: Int, obj: T,
        topicOverride: String? = null, qosOverride: QualityOfService? = null,
        dupOverride: Boolean? = null, retainOverride: Boolean? = null
    ) {
        val db = MqttDbProvider.getDb(app)
        val rowId = when (obj) {
            is SimpleModel -> {
                val id = db.modelsDao().insert(obj)
                val modelWithId = obj.copy(key = id)

                val qos = qosOverride ?: QualityOfService.AT_LEAST_ONCE
                val queue = MqttQueue(
                    SimpleModel::class.java.simpleName,
                    modelWithId.key, IPublishMessage.controlPacketValue, qos, connectionIdentifier
                )
                db.mqttQueueDao().publish(
                    queue,
                    topicOverride ?: "simple",
                    dupOverride ?: false,
                    retainOverride ?: false
                ) //Inject the real value
            }
            else -> throw MqttGeneratedCodeException(
                "Failed to publish $obj. Did you forget to annotate" +
                        " ${obj::class.java.canonicalName} with @MqttPublish?"
            )
        }
        notifyPublish(ClientToServiceConnection.NotifyPublish(connectionIdentifier, rowId))
    }

}


