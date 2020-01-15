package mqtt.client.session.transport.nio


import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.asCoroutineDispatcher
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.consumeAsFlow
import kotlinx.coroutines.flow.filterIsInstance
import kotlinx.coroutines.flow.take
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.isActive
import mqtt.client.block
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
import kotlin.time.measureTime
@ExperimentalTime
class AsyncClientControlPacketTransportIntegrationTests {

    private val timeoutOffsetMs = 150
    private val keepAliveTimeoutSeconds = 1
    private val integrationTestTimeoutMs = round(keepAliveTimeoutSeconds * 1.5).toInt() * 1000 + timeoutOffsetMs + 1

    val processors = Runtime.getRuntime().availableProcessors()
    val runCount = processors * 3

    val executors = Executors.newSingleThreadExecutor()
    val scope = CoroutineScope(executors.asCoroutineDispatcher())
    val provider = AsynchronousChannelGroup.withThreadPool(executors)!!

    fun connect(): ClientControlPacketTransport {
        println("processor count $processors $executors")
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
            println("Ping request run# $it/$runCount")
            val transport = connect()
            scope.blockWithTimeout(transport, integrationTestTimeoutMs.toLong() + timeoutOffsetMs) {
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
            }

            block {
                println("Disconnecting Ping request")
                delay(100)
                println("delay complete")
                disconnect(this, transport)
                println("delay done ping request")
            }
        }
    }

    @Test
    fun pingResponse() {
        repeat(runCount) {
            println("Ping response run# $it/$runCount")
            val transport = connect()
            scope.blockWithTimeout(
                transport,
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
            }
            blockWithTimeout(integrationTestTimeoutMs.toLong() + timeoutOffsetMs) {
                println("Disconnecting Ping response")
                delay(100)
                println("delay complete")
                disconnect(scope, transport)
                println("delay done ping response")
            }
        }
    }


    @ExperimentalTime
    suspend fun disconnect(scope: CoroutineScope, transport: ClientControlPacketTransport) {
        val completedWrite = transport.completedWrite
        if (completedWrite != null) {
            assert(completedWrite.isClosedForSend)
        }
        println("check isopen")
        var count = 0
        val time = measureTime {
            while (scope.isActive && transport.isOpen()) {
                count++
                println(count)
                delay(count.toLong())
            }
        }
        assertFalse(transport.isOpen())
        println("check assigned port $time $count")
        assertNull(transport.assignedPort(), "Leaked socket")
        println("validated assigned port")
        assert(transport.outboundChannel.isClosedForSend)
        println("outbound validated close")
        assert(transport.inboxChannel.isClosedForSend)
        println("inbox validated close")
    }

}

