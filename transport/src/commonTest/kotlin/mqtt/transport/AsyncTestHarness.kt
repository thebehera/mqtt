package mqtt.transport

import kotlinx.coroutines.*
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.consumeAsFlow
import kotlinx.coroutines.flow.filterIsInstance
import kotlinx.coroutines.flow.take
import kotlinx.coroutines.flow.toList
import mqtt.connection.ClientControlPacketTransport
import mqtt.wire.control.packet.ControlPacket
import mqtt.wire.control.packet.IPingRequest
import mqtt.wire.control.packet.IPingResponse
import mqtt.wire4.control.packet.ConnectionRequest
import kotlin.math.max
import kotlin.random.Random
import kotlin.test.*
import kotlin.time.ExperimentalTime
import kotlin.time.measureTime

@ExperimentalTime
expect fun asyncClientTestHarness(
    host: String?,
    port: Int? = null,
    runCount: Int,
    platformExtras: List<Any>,
    timeoutOffsetMs: Long = 150,
    keepAliveTimeoutSeconds: Int = 2,
    integrationTestTimeoutMs: Long = keepAliveTimeoutSeconds * 1000 + timeoutOffsetMs + 1
): AsyncClientTestHarness

@ExperimentalTime
abstract class AsyncClientTestHarness(
    val runCount: Int,
    val timeoutOffsetMs: Long = 150,
    val keepAliveTimeoutSeconds: Int = 2,
    val integrationTestTimeoutMs: Long = keepAliveTimeoutSeconds * 1000 + timeoutOffsetMs + 1
) {

    open val host: String? = null
    open val port: UShort? = null
    abstract val scope: CoroutineScope
    open val channelGroup: Any? = null

    abstract fun close()

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
        println("block connect with timeout")
        val t = aMqttClient(scope, connectionRequest, 12000, channelGroup)
        println("async client transport")
        assertTrue(t.open(60_000.toUShort()).isSuccessful, "incorrect connack message")
        transport = t
        assertNotNull(transport.assignedPort())
        return transport
    }

    fun pingRequest() {
        val jobs = ArrayList<Job>()
        repeat(runCount) {
            println("ping request $it / $runCount")
            try {
                jobs += scope.launch { pingRequestImpl() }
            } catch (e: Throwable) {
                println("error from pingRequest $it $e")
                throw e
            }
        }
        block { jobs.joinAll() }
    }

    fun pingResponse() {
        val jobs = ArrayList<Job>()
        repeat(runCount) {
            println("ping response st $it / $runCount")
            try {
                jobs += scope.launch { pingResponseImpl() }
            } catch (e: Throwable) {
                println("error from pingResponse $it $e")
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
            println("ping req connect")
            val transport = connect()
            println("ping req done connecting")
            val completedWriteChannel = Channel<ControlPacket>()
            transport.completedWrite = completedWriteChannel
            val expectedCount = max(
                1,
                integrationTestTimeoutMs / (transport.connectionRequest.keepAliveTimeoutSeconds.toInt() * 1000)
            )

            println("ping req consume")
            val responses =
                completedWriteChannel.consumeAsFlow().filterIsInstance<IPingRequest>().take(expectedCount.toInt())
                    .toList()
            println("ping consumed")
            assertEquals(expectedCount, responses.count().toLong())
            transport.suspendClose()
            disconnect(transport)
            println("ping request done $expectedCount")
        }
    }

    protected suspend fun pingResponseImpl() {
        if (!scope.isActive) {
            println("stopping pingResponseImpl not active")
            return
        }
        withTimeout((integrationTestTimeoutMs + timeoutOffsetMs) * 2) {
            val transport = connect()
            val expectedCount =
                max(
                    1,
                    integrationTestTimeoutMs / (transport.connectionRequest.keepAliveTimeoutSeconds.toInt() * 1000)
                )
            assertEquals(
                expectedCount.toInt(),
                transport.incomingControlPackets.filterIsInstance<IPingResponse>().take(expectedCount.toInt()).toList().count()
            )
            transport.suspendClose()
            disconnect(transport)
            println("ping response done $expectedCount")
        }
    }

    fun ultraAsync() {
        var count = 0
        val time = measureTime {
            val jobs = ArrayList<Job>()
            block(scope.coroutineContext) {
                repeat(runCount) {
                    jobs += launch {
                        println("ultra async ping request st $it / $runCount")
                        try {
                            println("ping req impl")
                            pingRequestImpl()
                            count++
                            println("ping req impl done")
                        } catch (e: Throwable) {
                            println("error from ultraAsyncTestSingleThreaded.pingRequestImpl $it $e")
                            throw e
                        }
                        pingRequestImpl()
                        count++
                    }
                    jobs += launch {
                        println("ultra async ping response st $it / $runCount")
                        try {
                            pingResponseImpl()
                            count++
                        } catch (e: Throwable) {
                            println("error from ultraAsyncTestSingleThreaded.pingResponseImpl $it $e")
                            throw e
                        }
                        pingResponseImpl()
                        count++
                    }
                }
                jobs.joinAll()
            }
        }
        println("completed ultra $count tests asynchrounously in $time")
    }

    @ExperimentalTime
    fun disconnect(transport: ClientControlPacketTransport) {
        val completedWrite = transport.completedWrite
        if (completedWrite != null) {
            assertTrue(completedWrite.isClosedForSend)
        }
        println("test isopen")
        assertFalse(transport.isOpen())
        assertNull(transport.assignedPort(), "Leaked socket")
        assertTrue(transport.outboundChannel.isClosedForSend)
        assertTrue((transport.inboxChannel as Channel<ControlPacket>).isClosedForSend)
    }
}