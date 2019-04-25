package mqtt.client

import mqtt.client.connection.ConnectionParameters
import mqtt.wire4.control.packet.ConnectionRequest
import kotlin.test.Test
import kotlin.test.assertEquals

class ClientTests {

    @Test
    fun canConnect() {
        val request = ConnectionRequest(getClientId())
        val invalidBadPort = 1
        val params = ConnectionParameters("localhost", invalidBadPort, false, request,
                maxNumberOfRetries = 3)
        val client = MqttClient(params)
        val job = client.startAsync()
        block {
            job.await()
        }
        assertEquals(3, client.connectionCount)
    }
}