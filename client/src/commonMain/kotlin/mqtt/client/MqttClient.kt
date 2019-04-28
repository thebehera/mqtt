package mqtt.client

import kotlinx.coroutines.*
import mqtt.client.connection.ConnectionParameters
import mqtt.client.connection.Open
import mqtt.client.platform.PlatformCoroutineDispatcher
import mqtt.client.session.ClientSession
import mqtt.client.session.ClientSessionState
import mqtt.wire.control.packet.findSerializer
import mqtt.wire.data.MqttUtf8String
import mqtt.wire.data.QualityOfService
import mqtt.wire4.control.packet.PublishMessage
import mqtt.wire4.control.packet.SubscribeRequest
import mqtt.wire4.control.packet.UnsubscribeRequest
import kotlin.coroutines.CoroutineContext

class MqttClient(val params: ConnectionParameters) : CoroutineScope {
    private val job: Job = Job()
    private val dispatcher = PlatformCoroutineDispatcher.dispatcher
    val state = ClientSessionState()
    override val coroutineContext: CoroutineContext = job + dispatcher
    var connectionCount = 0
    val session = ClientSession(params, Job(job), state)

    fun startAsync(newConnectionCb: Runnable? = null) = async {
        if (session.transport?.isOpenAndActive() == true) {
            return@async true
        }

        return@async retryIO(params.maxNumberOfRetries) {
            val result = try {
                if (isActive) {
                    val result = session.connect()
                    connectionCount++
                    newConnectionCb?.run()
                    session.awaitSocketClose()
                    result is Open
                } else {
                    false
                }
            } catch (e: Exception) {
                false
            }
            result
        }
    }

    inline fun <reified T : Any> publish(topic: String, qos: QualityOfService, payload: T?) {
        pub(topic, qos, payload)
    }

    inline fun <reified T : Any> pub(topic: String, qos: QualityOfService, payload: T?) {
        val actualPayload = if (payload == null) {
            null
        } else {
            val serializer = findSerializer<T>() ?: throw RuntimeException("Failed to find serializer for $payload")
            serializer.serialize(payload)
        }
        val publish = PublishMessage(topic, qos, actualPayload)
        launch { session.send(publish) }
    }

    fun <T> subscribe(topics: List<String>, qos: List<QualityOfService>) {
        val subscription = SubscribeRequest(topics, qos)
        launch { session.send(subscription) }

    }

    fun unsubscribe(topics: List<String>) {
        val unsubscribeRequest = UnsubscribeRequest(topics = topics.map { MqttUtf8String(it) })
        launch { session.send(unsubscribeRequest) }
    }

    fun stopAsync() = async {
        session.disconnectAsync()
    }
}