package mqtt.client

import kotlinx.coroutines.*
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
    override val coroutineContext: CoroutineContext = job + dispatcher
    var connectionCount = 0
    val session = ClientSession(params, Job(job))

    fun startAsync(newConnectionCb: Runnable? = null) = async {
        if (session.connection?.isOpenAndActive() == true) {
            return@async true
        }
        return@async retryIO(params.maxNumberOfRetries) {
            val result = try {
                if (isActive) {
                    val result = session.connectAsync().await()
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
        session.send(publish)
    }

    fun <T> subscribe(topics: List<String>, qos: List<QualityOfService>) {
        val subscription = SubscribeRequest(topics, qos)
        session.send(subscription)
    }

    fun unsubscribe(topics: List<String>) {
        val unsubscribeRequest = UnsubscribeRequest(topics = topics.map { MqttUtf8String(it) })
        session.send(unsubscribeRequest)
    }

    fun stop(): Deferred<Boolean>? {
        return session.disconnectAsync()
    }
}