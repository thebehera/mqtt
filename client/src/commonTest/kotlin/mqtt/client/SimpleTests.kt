package mqtt.client

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import mqtt.ApplicationMessageCallback
import mqtt.Client
import mqtt.connection.RemoteHost
import mqtt.wire.control.packet.IPublishMessage
import mqtt.wire.data.QualityOfService.AT_LEAST_ONCE
import mqtt.wire.data.QualityOfService.EXACTLY_ONCE
import mqtt.wire4.control.packet.ConnectionRequest
import kotlin.time.ExperimentalTime

@ExperimentalUnsignedTypes
@ExperimentalTime
class SimpleTests {

//    @Test
    fun mqttSimpleTest() = block {
        var request = ConnectionRequest<Unit>("123asoko0k234difhuio09123132344")
        val newVariableHeader = request.variableHeader.copy(keepAliveSeconds = 10)
        request = request.copy(newVariableHeader)
        val clientScope = CoroutineScope(Dispatchers.Default + this.coroutineContext)
        val remoteHost = RemoteHost("localhost", 1883, request)
        val client = Client(remoteHost, object : ApplicationMessageCallback {
            override suspend fun onPublishMessageReceived(client: Client, pub: IPublishMessage) {
                println("on pub received $pub")
                client.disconnect()
            }
        }, scope = clientScope)
        client.subscribe("rahul", AT_LEAST_ONCE)
        client.publish(topicName = "rahul23", payload = "testPayload", qos = EXACTLY_ONCE)
        client.connect()

    }
}