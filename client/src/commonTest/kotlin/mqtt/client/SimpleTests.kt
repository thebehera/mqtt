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
                ConnectionRequest<Unit>("m2${Random.nextLong()}", keepAliveSeconds = 56, cleanSession = true)
            ), scope = this
        )
        val topic = "hello-${Random.nextLong()}"
        println(client.connectAsync().await())
        println(client.publishAsync(topicName = topic, payload = "testP", qos = EXACTLY_ONCE).await())
        println(client.subscribeAsync(topic, AT_LEAST_ONCE).await())
        println(client.unsubscribeAsync(topic).await())
        println(client.disconnectAsync().await())
    }
}