package mqtt.client

import mqtt.connection.ConnectionOptions
import mqtt.wire.control.packet.*
import mqtt.wire.data.QualityOfService.AT_LEAST_ONCE
import mqtt.wire.data.QualityOfService.EXACTLY_ONCE
import mqtt.wire4.control.packet.ConnectionRequest
import kotlin.random.Random
import kotlin.test.assertTrue
import kotlin.time.ExperimentalTime

@ExperimentalUnsignedTypes
@ExperimentalTime
class SimpleTests {

//    @Test
    fun mqttSimpleTest() = block {
        println("start test")
        if (getNetworkCapabilities() == NetworkCapabilities.WEBSOCKETS_ONLY) return@block
        val client = Client(
            ConnectionOptions(
                "localhost",
                60_000,
                ConnectionRequest<Unit>("m2${Random.nextLong()}", cleanSession = true)
            ), scope = this
        )
        val topic = "hello-${Random.nextLong()}"
        println("connecting")
        assertTrue(IConnectionAcknowledgment::class.isInstance(client.connectAsync().await()))
        println("connected, publish")
        assertTrue(
            IPublishComplete::class.isInstance(
                client.publishAsync(
                    topicName = topic,
                    payload = "testP",
                    qos = EXACTLY_ONCE
                ).await()
            )
        )
        println("subscribe")
        assertTrue(ISubscribeAcknowledgement::class.isInstance(client.subscribeAsync(topic, AT_LEAST_ONCE).await()))
        println("unsub")
        assertTrue(IUnsubscribeAckowledgment::class.isInstance(client.unsubscribeAsync(topic).await()))
        println("disconnecting")
        assertTrue(IDisconnectNotification::class.isInstance(client.disconnectAsync().await()))
        println("disconnected")
    }

    //    @Test
    fun mqttOverWebsocketsSimpleTest() = block {
        println("start ws test")
        val client = Client(
            ConnectionOptions(
                "localhost",
                60_002,
                ConnectionRequest<Unit>("m2${Random.nextLong()}", keepAliveSeconds = 56, cleanSession = true),
                websocketEndpoint = "/mqtt"
            ), scope = this
        )
        val topic = "hello-${Random.nextLong()}"
        println("connecting")
        assertTrue(IConnectionAcknowledgment::class.isInstance(client.connectAsync().await()))
        println("connected, publish")
        assertTrue(
            IPublishComplete::class.isInstance(
                client.publishAsync(
                    topicName = topic,
                    payload = "testP",
                    qos = EXACTLY_ONCE
                ).await()
            )
        )
        println("subscribing")
        assertTrue(ISubscribeAcknowledgement::class.isInstance(client.subscribeAsync(topic, AT_LEAST_ONCE).await()))
        println("unsub")
        assertTrue(IUnsubscribeAckowledgment::class.isInstance(client.unsubscribeAsync(topic).await()))
        println("disconnecting")
        assertTrue(IDisconnectNotification::class.isInstance(client.disconnectAsync().await()))
        println("disconnected")
    }
}