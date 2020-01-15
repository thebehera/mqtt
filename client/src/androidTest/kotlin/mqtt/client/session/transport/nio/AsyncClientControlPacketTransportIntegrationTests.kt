package mqtt.client.session.transport.nio


import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.asCoroutineDispatcher
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
import java.nio.channels.AsynchronousChannelGroup
import java.util.concurrent.Executors
import kotlin.math.max
import kotlin.math.round
import kotlin.random.Random
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.time.ExperimentalTime

@ExperimentalTime
class AsyncClientControlPacketTransportIntegrationTests {

    private val timeoutOffsetMs = 150
    private val keepAliveTimeoutSeconds = 1
    private val integrationTestTimeoutMs = round(keepAliveTimeoutSeconds * 1.5).toInt() * 1000 + timeoutOffsetMs + 1

    val processors = Runtime.getRuntime().availableProcessors()
    val runCount = processors / 2

    val executors = Executors.newSingleThreadExecutor()
    val scope = CoroutineScope(executors.asCoroutineDispatcher())
    val provider = AsynchronousChannelGroup.withThreadPool(executors)!!

    fun connect(): ClientControlPacketTransport {
        val connectionRequest = ConnectionRequest(
            clientId = "test${Random.nextInt()}",
            keepAliveSeconds = keepAliveTimeoutSeconds.toUShort()
        )
        assert(integrationTestTimeoutMs > connectionRequest.keepAliveTimeoutSeconds.toInt() * 1000 + timeoutOffsetMs) { "Integration timeout too low" }
        var transport: ClientControlPacketTransport? = null
        scope.blockWithTimeout(timeoutOffsetMs.toLong()) {
            val t = asyncClientTransport(scope, connectionRequest, provider)
            assert(t.open(60_000.toUShort()).isSuccessful) { "incorrect connack message" }
            transport = t
        }
        assertNotNull(transport!!.assignedPort())
        return transport!!
    }

    @Test
    fun pingRequest() {
        repeat(runCount) {
            val transport = connect()
            scope.blockWithTimeout(integrationTestTimeoutMs.toLong() + timeoutOffsetMs) {
                val completedWriteChannel = Channel<ControlPacket>()
                transport.completedWrite = completedWriteChannel
                val expectedCount = max(
                    1,
                    integrationTestTimeoutMs / (transport.connectionRequest.keepAliveTimeoutSeconds.toInt() * 1000)
                )
                assertEquals(
                    expectedCount,
                    completedWriteChannel.consumeAsFlow().filterIsInstance<IPingRequest>().take(expectedCount).toList().count()
                )
                transport.suspendClose()
            }
            disconnect(transport)
        }
    }

    @Test
    fun pingResponse() {
        repeat(runCount) {
            val transport = connect()
            scope.blockWithTimeout(
                integrationTestTimeoutMs.toLong() + timeoutOffsetMs
            ) {
                val expectedCount =
                    max(
                        1,
                        integrationTestTimeoutMs / (transport.connectionRequest.keepAliveTimeoutSeconds.toInt() * 1000)
                    )
                assertEquals(
                    expectedCount,
                    transport.incomingControlPackets.filterIsInstance<IPingResponse>().take(expectedCount).toList().count()
                )
                transport.suspendClose()
            }
            disconnect(transport)
        }
    }


    @ExperimentalTime
    fun disconnect(transport: ClientControlPacketTransport) {
        val completedWrite = transport.completedWrite
        if (completedWrite != null) {
            assert(completedWrite.isClosedForSend)
        }
        assertFalse(transport.isOpen())
        assertNull(transport.assignedPort(), "Leaked socket")
        assert(transport.outboundChannel.isClosedForSend)
        assert(transport.inboxChannel.isClosedForSend)
    }

}

