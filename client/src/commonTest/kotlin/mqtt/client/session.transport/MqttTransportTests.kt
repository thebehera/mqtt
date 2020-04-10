@file:Suppress("EXPERIMENTAL_API_USAGE")

package mqtt.client.session.transport

import kotlin.time.ExperimentalTime

@ExperimentalTime
class MqttTransportTests {
//    fun createConnectionRequest(clientId: String = "test"): Pair<ConnectionRequest, RemoteHost> {
//        val request = ConnectionRequest(clientId, keepAliveSeconds = 10.toUShort())
//        val host = RemoteHost(
//            "localhost",
//            port = port,
//            request = request,
//            security = RemoteHost.Security(
//                isTransportLayerSecurityEnabled = false
//            ),
//            maxNumberOfRetries = 3
//        )
//        return Pair(request, host)
//    }
//
//    val pool by lazy {
//        BufferPool(object : BufferMemoryLimit {
//            override fun isTooLargeForMemory(size: UInt) = false
//        })
//    }
//
//    @Test
//    fun connectDisconnect() = blockWithTimeout {
//        val (connectionRequest, remoteHost) = createConnectionRequest()
//        val (liveSession, connack) = MqttTransport.openConnection(this, remoteHost, pool)
//        liveSession.asyncWrite(connectionRequest)
//        assertTrue(liveSession.incomingPackets().first() is IConnectionAcknowledgment)
//        liveSession.asyncWrite(DisconnectNotification)
//        liveSession.socket.close()
//        assertFalse(liveSession.socket.isOpen())
//    }
}