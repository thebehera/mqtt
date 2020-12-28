package mqtt.client

import mqtt.Client
import mqtt.connection.RemoteHost
import mqtt.wire.data.QualityOfService.AT_LEAST_ONCE
import mqtt.wire.data.QualityOfService.EXACTLY_ONCE
import mqtt.wire4.control.packet.ConnectionRequest
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
                ConnectionRequest<Unit>("mix12562", keepAliveSeconds = 56, cleanSession = true)
            ), scope = this
        )
        client.connectAsync().await()
        client.subscribeAsync("rahul12", AT_LEAST_ONCE).await()
        client.publishAsync(topicName = "rahul12", payload = "testPayloa2d2", qos = EXACTLY_ONCE).await()
        client.unsubscribeAsync("rahul21").await()
        client.disconnectAsync().await()
    }
}