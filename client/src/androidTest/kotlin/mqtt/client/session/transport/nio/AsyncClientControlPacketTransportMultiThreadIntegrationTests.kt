package mqtt.client.session.transport.nio


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
import org.junit.After
import org.junit.Before
import org.junit.Test
import java.nio.channels.AsynchronousChannelGroup
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import kotlin.math.max
import kotlin.random.Random
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.time.ExperimentalTime
import kotlin.time.measureTime

@ExperimentalTime
class AsyncClientControlPacketTransportMultiThreadIntegrationTests {

    private val timeoutOffsetMs = 150
    private val keepAliveTimeoutSeconds = 2
    private val integrationTestTimeoutMs = keepAliveTimeoutSeconds * 1000 + timeoutOffsetMs + 1

    val processors = Runtime.getRuntime().availableProcessors()
    val runCount = processors * 2
    lateinit var multiThreadExecutor: ExecutorService
    lateinit var multiThreadScope: CoroutineScope
    lateinit var multiThreadProvider: AsynchronousChannelGroup


    suspend fun connect(
        scope: CoroutineScope,
        channelGroup: AsynchronousChannelGroup? = null
    ): ClientControlPacketTransport {

        val connectionRequest = ConnectionRequest(
            clientId = "test${Random.nextInt()}",
            keepAliveSeconds = keepAliveTimeoutSeconds.toUShort()
        )
        assert(integrationTestTimeoutMs > connectionRequest.keepAliveTimeoutSeconds.toInt() * 1000 + timeoutOffsetMs) { "Integration timeout too low" }
        var transport: ClientControlPacketTransport? = null
        println("block connect with timeout")
        val t = asyncClientTransport(scope, connectionRequest, channelGroup)
        println("async client transport")
        assert(t.open(60_000.toUShort()).isSuccessful) { "incorrect connack message" }
        transport = t
        assertNotNull(transport.assignedPort())
        return transport
    }


    @Test
    fun pingRequestMultiThread() {
        repeat(runCount) {
            println("ping request mt $it / $runCount")
            try {
                runBlocking { pingRequestImpl(multiThreadScope, multiThreadProvider) }
            } catch (e: Throwable) {
                println("error from pingRequestMultiThread $it")
                e.printStackTrace()
                throw e
            }
        }
    }

    @Test
    fun pingResponseMultiThreaded() {
        repeat(runCount) {
            println("ping response mt $it / $runCount")
            try {
                runBlocking { pingResponseImpl(multiThreadScope, multiThreadProvider) }
            } catch (e: Throwable) {
                println("error from pingResponseMultiThreaded $it")
                e.printStackTrace()
                throw e
            }
        }
    }

    suspend fun pingRequestImpl(scope: CoroutineScope, channelGroup: AsynchronousChannelGroup? = null) {
        if (!scope.isActive) {
            println("stopping pingRequestImpl not active")
            return
        }
        withTimeout((integrationTestTimeoutMs.toLong() + timeoutOffsetMs) * 2) {
            println("ping req connect")
            val transport = connect(scope, channelGroup)
            println("ping req done connecting")
            val completedWriteChannel = Channel<ControlPacket>()
            transport.completedWrite = completedWriteChannel
            val expectedCount = max(
                1,
                integrationTestTimeoutMs / (transport.connectionRequest.keepAliveTimeoutSeconds.toInt() * 1000)
            )

            println("ping req consume $expectedCount")
            val responses: List<IPingRequest>
            val time = measureTime {
                responses =
                    completedWriteChannel.consumeAsFlow().filterIsInstance<IPingRequest>().take(expectedCount).toList()
            }
            println("ping consumed $time")
            assertEquals(expectedCount, responses.count())
            transport.suspendClose()
            transport.close()
            disconnect(transport)
            println("ping request done")
        }
    }

    suspend fun pingResponseImpl(scope: CoroutineScope, channelGroup: AsynchronousChannelGroup? = null) {
        if (!scope.isActive) {
            println("stopping pingResponseImpl not active")
            return
        }
        withTimeout((integrationTestTimeoutMs.toLong() + timeoutOffsetMs) * 2) {
            val transport = connect(scope, channelGroup)
            val expectedCount =
                max(
                    1,
                    integrationTestTimeoutMs / (transport.connectionRequest.keepAliveTimeoutSeconds.toInt() * 1000)
                )
            val time = measureTime {
                assertEquals(
                    expectedCount,
                    transport.incomingControlPackets.filterIsInstance<IPingResponse>().take(expectedCount).toList().count()
                )
            }
            println("ping response impl $time")
            transport.suspendClose()
            transport.close()
            disconnect(transport)
            println("ping response done")
        }
    }

    @Test
    fun ultraAsyncTestMultiThreaded() {
        runBlocking(multiThreadScope.coroutineContext) {
            repeat(runCount) {
                delay(runCount * 50.toLong())
                launch {
                    println("ultra async ping request mt $it / $runCount")
                    try {
                        pingRequestImpl(multiThreadScope, multiThreadProvider)
                    } catch (e: Throwable) {
                        println("error from ultraAsyncTestMultiThreaded.pingRequestImpl $it")
                        e.printStackTrace()
                        throw e
                    }
                    println("ultra async ping request done mt $it / $runCount")
                }
                delay(runCount * 50.toLong())
                launch {
                    println("ultra async ping response mt $it / $runCount")
                    try {
                        pingResponseImpl(multiThreadScope, multiThreadProvider)
                    } catch (e: Throwable) {
                        println("error from ultraAsyncTestMultiThreaded.pingResponseImpl $it")
                        e.printStackTrace()
                        throw e
                    }
                    println("ultra async ping response mt $it / $runCount")
                }
            }
        }
    }

    @ExperimentalTime
    fun disconnect(transport: ClientControlPacketTransport) {
        val completedWrite = transport.completedWrite
        if (completedWrite != null) {
            assert(completedWrite.isClosedForSend)
        }
        println("test isopen")
        assertFalse(transport.isOpen())
        assertNull(transport.assignedPort(), "Leaked socket")
        assert(transport.outboundChannel.isClosedForSend)
        assert(transport.inboxChannel.isClosedForSend)
    }


    @Before
    fun reset() {
        multiThreadExecutor = Executors.newFixedThreadPool(runCount * 2)
        multiThreadScope = CoroutineScope(multiThreadExecutor.asCoroutineDispatcher())
        multiThreadProvider = AsynchronousChannelGroup.withThreadPool(multiThreadExecutor)
    }

    @After
    fun close() {
        println("cancel multi thread scope")
        multiThreadScope.cancel()
        println("shut down multi thread provider ${multiThreadProvider.shutdownNow()}")
        println("shut down multi thread executor ${multiThreadExecutor.shutdownNow()}")

        val multiThreadAwaited = multiThreadProvider.awaitTermination(timeoutOffsetMs.toLong(), TimeUnit.MILLISECONDS)
        println("mt provider awaited $multiThreadAwaited, awaiting st executor")
        val multiExecutorAwaited = multiThreadExecutor.awaitTermination(timeoutOffsetMs.toLong(), TimeUnit.MILLISECONDS)
        println("mt executor awaited $multiExecutorAwaited, awaiting mt executor")

    }

}

