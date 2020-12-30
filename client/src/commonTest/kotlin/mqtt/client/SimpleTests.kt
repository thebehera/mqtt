package mqtt.client

import mqtt.Client
import mqtt.connection.RemoteHost
import mqtt.wire.control.packet.*
import mqtt.wire.data.QualityOfService.AT_LEAST_ONCE
import mqtt.wire.data.QualityOfService.EXACTLY_ONCE
import mqtt.wire4.control.packet.ConnectionRequest
import kotlin.random.Random
import kotlin.test.Test
import kotlin.test.assertTrue
import kotlin.time.ExperimentalTime

@ExperimentalUnsignedTypes
@ExperimentalTime
class SimpleTests {

    @Test
    fun mqttSimpleTest() = block {
        val client = Client(
            RemoteHost(
                "test.mosquitto.org",
                1883,
                ConnectionRequest<Unit>("m2${Random.nextLong()}", cleanSession = true)
            ), scope = this
        )
        val topic = "hello-${Random.nextLong()}"
        assertTrue(IConnectionAcknowledgment::class.isInstance(client.connectAsync().await()))
        assertTrue(
            IPublishComplete::class.isInstance(
                client.publishAsync(
                    topicName = topic,
                    payload = "testP",
                    qos = EXACTLY_ONCE
                ).await()
            )
        )
        assertTrue(ISubscribeAcknowledgement::class.isInstance(client.subscribeAsync(topic, AT_LEAST_ONCE).await()))
        assertTrue(IUnsubscribeAckowledgment::class.isInstance(client.unsubscribeAsync(topic).await()))
        assertTrue(IDisconnectNotification::class.isInstance(client.disconnectAsync().await()))
    }

    @Test
    fun mqttOverWebsocketsSimpleTest() = block {
        val client = Client(
            RemoteHost(
                "test.mosquitto.org",
                8080,
                ConnectionRequest<Unit>("m2${Random.nextLong()}", keepAliveSeconds = 56, cleanSession = true),
                websocket = RemoteHost.WebsocketParameters("/mqtt")
            ), scope = this
        )
        val topic = "hello-${Random.nextLong()}"
        assertTrue(IConnectionAcknowledgment::class.isInstance(client.connectAsync().await()))
        assertTrue(
            IPublishComplete::class.isInstance(
                client.publishAsync(
                    topicName = topic,
                    payload = "testP",
                    qos = EXACTLY_ONCE
                ).await()
            )
        )
        assertTrue(ISubscribeAcknowledgement::class.isInstance(client.subscribeAsync(topic, AT_LEAST_ONCE).await()))
        assertTrue(IUnsubscribeAckowledgment::class.isInstance(client.unsubscribeAsync(topic).await()))
        assertTrue(IDisconnectNotification::class.isInstance(client.disconnectAsync().await()))
    }
}