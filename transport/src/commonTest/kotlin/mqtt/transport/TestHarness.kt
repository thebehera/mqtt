package mqtt.transport

import kotlinx.coroutines.*
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.consumeAsFlow
import kotlinx.coroutines.flow.filterIsInstance
import kotlinx.coroutines.flow.take
import kotlinx.coroutines.flow.toList
import mqtt.connection.ClientControlPacketTransport
import mqtt.wire.control.packet.ControlPacket
import mqtt.wire.control.packet.IConnectionRequest
import mqtt.wire.control.packet.IPingRequest
import mqtt.wire.control.packet.IPingResponse
import mqtt.wire4.control.packet.ConnectionRequest
import mqtt.wire4.control.packet.PingRequest
import kotlin.math.max
import kotlin.random.Random
import kotlin.test.*
import kotlin.time.ExperimentalTime
import kotlin.time.measureTime

@ExperimentalTime
abstract class TestHarness(
    val runCount: Int,
    val timeoutOffsetMs: Long = 150,
    val keepAliveTimeoutSeconds: Int = 2,
    val integrationTestTimeoutMs: Long = keepAliveTimeoutSeconds * 1000 + timeoutOffsetMs + 1
) {

    open val host: String? = null
    open val port: UShort? = null
    abstract val scope: CoroutineScope
    open val channelGroup: Any? = null

    open fun close() {}

    abstract suspend fun createClient(connectionRequest: IConnectionRequest): ClientControlPacketTransport
    @ExperimentalTime
    suspend fun connect(): ClientControlPacketTransport {

        val connectionRequest = ConnectionRequest(
            clientId = "test${Random.nextInt()}",
            keepAliveSeconds = keepAliveTimeoutSeconds.toUShort()
        )
        assertTrue(
            integrationTestTimeoutMs > connectionRequest.keepAliveTimeoutSeconds.toInt() * 1000 + timeoutOffsetMs,
            "Integration timeout too low"
        )
        var transport: ClientControlPacketTransport? = null
        val t = createClient(connectionRequest)
        assertTrue(t.open(60_000.toUShort()).isSuccessful, "incorrect connack message")
        transport = t
        assertNotNull(transport.assignedPort())
        return transport
    }

    open fun pingRequest() {
        val jobs = ArrayList<Job>()
        repeat(runCount) {
            try {
                jobs += scope.launch { pingRequestImpl() }
            } catch (e: Throwable) {
                println("error from pingRequest $it/ $runCount $e")
                throw e
            }
        }
        block { jobs.joinAll() }
    }

    open fun pingResponse() {
        val jobs = ArrayList<Job>()
        repeat(runCount) {
            try {
                jobs += scope.launch { pingResponseImpl() }
            } catch (e: Throwable) {
                println("error from pingResponse $it / $runCount $e")
                throw e
            }
        }
        block { jobs.joinAll() }
    }

    protected suspend fun pingRequestImpl() {
        if (!scope.isActive) {
            println("stopping pingRequestImpl not active")
            return
        }
        withTimeout((integrationTestTimeoutMs + timeoutOffsetMs) * 2) {
            val transport = try {
                connect()
            } catch (e: Exception) {
                delay(1)
                try {
                    connect()
                } catch (e2: Exception) {
                    println("Still failed after $e, aborting $e2")
                    throw e2
                }
            }

            val completedWriteChannel =
                Channel<ControlPacket>()
            transport.completedWrite = completedWriteChannel
            val expectedCount = max(
                1,
                integrationTestTimeoutMs / (transport.connectionRequest.keepAliveTimeoutSeconds.toInt() * 1000)
            )

            repeat(expectedCount.toInt()) {
                transport.outboundChannel.send(PingRequest)
            }
            val responses =
                completedWriteChannel.consumeAsFlow()
                    .filterIsInstance<IPingRequest>()
                    .take(expectedCount.toInt())
                    .toList()
            assertEquals(expectedCount, responses.count().toLong())
            transport.suspendClose()
            disconnect(transport)
        }
    }

    protected suspend fun pingResponseImpl() {
        if (!scope.isActive) {
            println("stopping pingResponseImpl not active")
            return
        }
        withTimeout((integrationTestTimeoutMs + timeoutOffsetMs) * 2) {
            val transport = try {
                connect()
            } catch (e: Exception) {
                delay(1)
                connect()
            }
            val expectedCount =
                max(
                    1,
                    integrationTestTimeoutMs / (transport.connectionRequest.keepAliveTimeoutSeconds.toInt() * 1000)
                )
            repeat(expectedCount.toInt()) {
                transport.outboundChannel.send(PingRequest)
            }
            assertEquals(
                expectedCount.toInt(),
                transport.incomingControlPackets.filterIsInstance<IPingResponse>().take(
                    expectedCount.toInt()
                ).toList().count()
            )
            transport.suspendClose()
            disconnect(transport)
        }
    }

    fun ultraAsync() {
        var count = 0
        val time = measureTime {
            val jobs = ArrayList<Job>()
            block(scope.coroutineContext) {
                repeat(runCount) {
                    jobs += launch {
                        try {
                            pingRequestImpl()
                            count++
                        } catch (e: Throwable) {
                            println("error from ultraAsyncTestSingleThreaded.pingRequestImpl $it  / $runCount  $count $e from ${e.cause}, trying one more time")
                            pingRequestImpl()
                        }
                        count++
                    }
                    jobs += launch {
                        try {
                            pingResponseImpl()
                            count++
                        } catch (e: Throwable) {
                            println("error from ultraAsyncTestSingleThreaded.pingResponseImpl $it  / $runCount $e  $count from ${e.cause}, trying one more time")
                            pingResponseImpl()
                        }
                        count++
                    }
                }
                jobs.joinAll()
            }
        }
        println("completed ultra $count tests asynchrounously in $time")
    }

    @ExperimentalTime
    open fun disconnect(transport: ClientControlPacketTransport) {
        val completedWrite = transport.completedWrite
        if (completedWrite != null) {
            assertTrue(completedWrite.isClosedForSend)
        }
        assertFalse(transport.isOpen())
        assertNull(transport.assignedPort(), "Leaked socket")
        assertTrue(transport.outboundChannel.isClosedForSend)
        assertTrue((transport.inboxChannel as Channel<ControlPacket>).isClosedForSend)
    }
}