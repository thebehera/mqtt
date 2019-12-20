package mqtt.client.session.transport

import mqtt.client.MqttClient
import mqtt.client.RemoteHost
import mqtt.client.getClientId
import mqtt.client.port
import mqtt.wire4.control.packet.ConnectionRequest
import kotlin.test.Test

class SocketTransportTests {
    @Test
    fun openConnectionTest() {
        val request = ConnectionRequest(getClientId(), keepAliveSeconds = 10.toUShort())
        val client = MqttClient(
            RemoteHost(
                "localhost",
                port = port,
                request = request,
                security = RemoteHost.Security(
                    isTransportLayerSecurityEnabled = false
                ),
                maxNumberOfRetries = 3
            )
        )
    }
}