package mqtt.client.session.transport.nio

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.asCoroutineDispatcher
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
import org.junit.Test
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import kotlin.math.max
import kotlin.random.Random
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.time.ExperimentalTime

@ExperimentalTime
class AsyncClientControlPacketTransportIntegrationTests {

    private val integrationTestTimeout = 4200
    private val timeoutOffset = 150

    val executors = Executors.newSingleThreadExecutor()

    fun connect(): Pair<CoroutineScope, ClientControlPacketTransport> {
        val scope = CoroutineScope(executors.asCoroutineDispatcher())
        val connectionRequest = ConnectionRequest(clientId = "test${Random.nextInt()}", keepAliveSeconds = 1.toUShort())
        assert(integrationTestTimeout > connectionRequest.keepAliveTimeoutSeconds.toInt() * 1000 + timeoutOffset) { "Integration timeout too low" }
        var transport: ClientControlPacketTransport? = null
        scope.blockWithTimeout(timeoutOffset.toLong()) {
            val t = asyncClientTransport(scope, connectionRequest)
            assert(t.open(60_000.toUShort()).isSuccessful) { "incorrect connack message" }
            transport = t
        }
        assertNotNull(transport!!.assignedPort())
        return Pair(scope, transport!!)
    }

    @Test
    fun pingRequest() {
        repeat(5) {
            val (scope, transport) = connect()
            scope.blockWithTimeout(transport, integrationTestTimeout.toLong() + timeoutOffset) {
                val completedWriteChannel = Channel<ControlPacket>()
                transport.completedWrite = completedWriteChannel
                val expectedCount = max(
                    1,
                    integrationTestTimeout / (transport.connectionRequest.keepAliveTimeoutSeconds.toInt() * 1000)
                )
                assertEquals(
                    expectedCount,
                    completedWriteChannel.consumeAsFlow().filterIsInstance<IPingRequest>().take(expectedCount).toList().count()
                )
            }
            disconnect(scope, transport)
        }
    }

    @Test
    fun pingResponse() {
        repeat(5) {
            val (scope, transport) = connect()
            scope.blockWithTimeout(
                transport,
                integrationTestTimeout.toLong() + timeoutOffset
            ) {
                val expectedCount =
                    max(
                        1,
                        integrationTestTimeout / (transport.connectionRequest.keepAliveTimeoutSeconds.toInt() * 1000)
                    )
                assertEquals(
                    expectedCount,
                    transport.incomingControlPackets.filterIsInstance<IPingResponse>().take(expectedCount).toList().count()
                )

            }
            disconnect(scope, transport)
        }
    }

    fun disconnect(scope: CoroutineScope, transport: ClientControlPacketTransport) {
        transport.close()
        val completedWrite = transport.completedWrite
        if (completedWrite != null) {
            assert(completedWrite.isClosedForSend)
        }
        assert(transport.outboundChannel.isClosedForSend)
        assert(transport.inboxChannel.isClosedForSend)
        assertNull(transport.assignedPort(), "Leaked socket")
        scope.cancel()
        executors.awaitTermination(timeoutOffset.toLong(), TimeUnit.MILLISECONDS)
    }
}

