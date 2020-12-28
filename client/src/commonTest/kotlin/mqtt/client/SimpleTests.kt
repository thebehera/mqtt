package mqtt.client

import mqtt.Client
import mqtt.connection.RemoteHost
import mqtt.wire.data.QualityOfService.AT_LEAST_ONCE
import mqtt.wire.data.QualityOfService.EXACTLY_ONCE
import mqtt.wire4.control.packet.ConnectionRequest
import kotlin.random.Random
import kotlin.test.Test
import kotlin.time.ExperimentalTime

@ExperimentalUnsignedTypes
@ExperimentalTime
class SimpleTests {

    @Test
    fun mqttSimpleTest() = block {
        val client = Client(
            RemoteHost(
                "localhost",
                1883,
                ConnectionRequest<Unit>("mix12${Random.nextLong()}", keepAliveSeconds = 56, cleanSession = true)
            ), scope = this
        )
        println("connecting")
        client.connectAsync().await()
        println("connected, subscribe")
        client.subscribeAsync("rahul12", AT_LEAST_ONCE).await()
        println("subscribed, publish")
        client.publishAsync(topicName = "rahul12", payload = "testPayloa2d2", qos = EXACTLY_ONCE).await()
        println("published, unsubscibe")
        client.unsubscribeAsync("rahul21").await()
        println("unsubscribe, disconnect")
        client.disconnectAsync().await()
        println("disconencted")
    }
}