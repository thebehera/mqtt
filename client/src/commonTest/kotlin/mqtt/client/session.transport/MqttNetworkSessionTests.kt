@file:Suppress("EXPERIMENTAL_API_USAGE")

package mqtt.client.session.transport

import kotlinx.coroutines.flow.first
import mqtt.buffer.BufferMemoryLimit
import mqtt.buffer.BufferPool
import mqtt.client.RemoteHost
import mqtt.client.blockWithTimeout
import mqtt.client.getClientId
import mqtt.client.port
import mqtt.wire.control.packet.IConnectionAcknowledgment
import mqtt.wire4.control.packet.ConnectionRequest
import mqtt.wire4.control.packet.ControlPacketV4Reader
import mqtt.wire4.control.packet.DisconnectNotification
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import kotlin.time.ExperimentalTime

@ExperimentalTime
class MqttNetworkSessionTests {
    fun createConnectionRequest(clientId: String = getClientId()): Pair<ConnectionRequest, RemoteHost> {
        val request = ConnectionRequest(clientId, keepAliveSeconds = 10.toUShort())
        val host = RemoteHost(
            "localhost",
            port = port,
            request = request,
            security = RemoteHost.Security(
                isTransportLayerSecurityEnabled = false
            ),
            maxNumberOfRetries = 3
        )
        return Pair(request, host)
    }

    val pool by lazy {
        BufferPool(object : BufferMemoryLimit {
            override fun isTooLargeForMemory(size: UInt) = false
        })
    }

    @Test
    fun connectDisconnect() = blockWithTimeout {
        val (connectionRequest, remoteHost) = createConnectionRequest()
        val liveSession = MqttNetworkSession.openConnection(this, remoteHost, pool)
        liveSession.asyncWrite(connectionRequest)
        assertTrue(liveSession.incomingPackets().first() is IConnectionAcknowledgment)
        liveSession.asyncWrite(DisconnectNotification)
        liveSession.socket.close()
        assertFalse(liveSession.socket.isOpen())
    }
}