package mqtt.client.session.transport.nio

import kotlinx.coroutines.*
import kotlinx.coroutines.flow.collect
import mqtt.client.blockWithTimeout
import mqtt.connection.ClientControlPacketTransport
import mqtt.wire4.control.packet.ConnectionRequest
import org.junit.After
import org.junit.Before
import org.junit.Test
import kotlin.random.Random

class AsyncClientControlPacketTransportIntegrationTests {

    lateinit var scope: CoroutineScope
    private lateinit var transport: ClientControlPacketTransport
    private val timeoutMs = 10_000L

    @Before
    fun setupConnection() {
        scope = CoroutineScope(Job() + Dispatchers.Unconfined)
        blockWithTimeout { }
        val connectionRequest = ConnectionRequest(clientId = "test${Random.nextInt()}", keepAliveSeconds = 2.toUShort())
        scope.blockWithTimeout(75) {
            transport = asyncClientTransport(connectionRequest, scope)
            println("connack ${transport.open(60_000)}")
        }
    }

    @Test
    fun testAsyncConnection() = scope.blockWithTimeout(transport, timeoutMs) {
        scope.launch {
            transport.incomingControlPackets.collect { incomingPacket ->
                println("reading $incomingPacket")
            }
        }
        delay(timeoutMs - 1000)
    }

    @After
    fun cleanup() {
        scope.cancel()
    }
}

