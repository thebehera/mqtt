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
                60000,
                ConnectionRequest<Unit>("m2${Random.nextLong()}", keepAliveSeconds = 56, cleanSession = true)
            ), scope = this
        )
        val topic = "hello-${Random.nextLong()}"
        client.connectAsync().await()
        client.publishAsync(topicName = topic, payload = "testP", qos = EXACTLY_ONCE).await()
        client.subscribeAsync(topic, AT_LEAST_ONCE).await()
        client.unsubscribeAsync(topic).await()
        client.disconnectAsync()
    }
}