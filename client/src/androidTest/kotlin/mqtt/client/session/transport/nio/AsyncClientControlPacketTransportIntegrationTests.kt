package mqtt.client.session.transport.nio

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.consumeAsFlow
import kotlinx.coroutines.flow.filterIsInstance
import kotlinx.coroutines.flow.take
import kotlinx.coroutines.flow.toList
import mqtt.client.blockWithTimeout
import mqtt.connection.ClientControlPacketTransport
import mqtt.wire.control.packet.ControlPacket
import mqtt.wire.control.packet.IPingRequest
import mqtt.wire.control.packet.IPingResponse
import mqtt.wire4.control.packet.ConnectionRequest
import org.junit.After
import org.junit.Before
import org.junit.Test
import kotlin.math.max
import kotlin.random.Random
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.time.ExperimentalTime

@ExperimentalTime
class AsyncClientControlPacketTransportIntegrationTests {

    lateinit var scope: CoroutineScope
    private lateinit var transport: ClientControlPacketTransport
    private val integrationTestTimeout = 2101
    private val timeoutOffset = 100

    @Before
    fun connect() {
        scope = CoroutineScope(Job() + Dispatchers.Default)
        val connectionRequest = ConnectionRequest(clientId = "test${Random.nextInt()}", keepAliveSeconds = 2.toUShort())
        assert(integrationTestTimeout > connectionRequest.keepAliveTimeoutSeconds.toInt() * 1000 + timeoutOffset) { "Integration timeout too low" }
        scope.blockWithTimeout(timeoutOffset.toLong()) {
            val transport = asyncClientTransport(connectionRequest, scope)
            assert(transport.open(60_000.toUShort()).isSuccessful) { "incorrect connack message" }
            this@AsyncClientControlPacketTransportIntegrationTests.transport = transport
        }
        assertNotNull(transport.assignedPort())
    }

    @Test
    fun pingRequest() = CoroutineScope(Dispatchers.Default)
        .blockWithTimeout(transport, integrationTestTimeout.toLong() + timeoutOffset) {
            val completedWriteChannel = Channel<ControlPacket>()
            transport.completedWrite = completedWriteChannel
            val expectedCount =
                max(1, integrationTestTimeout / (transport.connectionRequest.keepAliveTimeoutSeconds.toInt() * 1000))
            assertEquals(
                expectedCount,
                completedWriteChannel.consumeAsFlow().filterIsInstance<IPingRequest>().take(expectedCount).toList().count()
            )
        }

    @Test
    fun pingResponse() = CoroutineScope(Dispatchers.Default)
        .blockWithTimeout(transport, integrationTestTimeout.toLong() + timeoutOffset) {
            val expectedCount =
                max(1, integrationTestTimeout / (transport.connectionRequest.keepAliveTimeoutSeconds.toInt() * 1000))
            assertEquals(
                expectedCount,
                transport.incomingControlPackets.filterIsInstance<IPingResponse>().take(expectedCount).toList().count()
            )
        }

    @After
    fun disconnect() {
        val completedWrite = transport.completedWrite
        if (completedWrite != null) {
            assert(completedWrite.isClosedForSend)
        }
        assert(transport.outboundChannel.isClosedForSend)
        scope.cancel()
        assertNull(transport.assignedPort(), "Leaked socket")
    }
}

