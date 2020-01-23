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
import kotlin.test.*
import kotlin.time.ExperimentalTime

@ExperimentalTime
class AsyncClientControlPacketTransportIntegrationTests {

    private val timeoutOffsetMs = 150
    private val keepAliveTimeoutSeconds = 2
    private val integrationTestTimeoutMs = keepAliveTimeoutSeconds * 1000 + timeoutOffsetMs + 1

    val processors = Runtime.getRuntime().availableProcessors()
    val runCount = processors * 2

    lateinit var singleThreadExecutor: ExecutorService
    lateinit var singleThreadScope: CoroutineScope
    lateinit var singleThreadProvider: AsynchronousChannelGroup

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
    fun pingRequestSingleThread() {
        repeat(runCount) {
            println("ping request st $it / $runCount")
            try {
                runBlocking { pingRequestImpl(singleThreadScope, singleThreadProvider) }
            } catch (e: Throwable) {
                println("error from pingRequestSingleThread $it")
                e.printStackTrace()
                throw e
            }
        }
    }


    @Test
    fun pingResponseSingleThread() {
        repeat(runCount) {
            println("ping response st $it / $runCount")
            try {
                runBlocking { pingResponseImpl(singleThreadScope, singleThreadProvider) }
            } catch (e: Throwable) {
                println("error from pingResponseSingleThread $it")
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
        delay(50)
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

            println("ping req consume")
            val responses =
                completedWriteChannel.consumeAsFlow().filterIsInstance<IPingRequest>().take(expectedCount).toList()
            println("ping consumed")
            assertEquals(expectedCount, responses.count())
            transport.suspendClose()
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
            assertEquals(
                expectedCount,
                transport.incomingControlPackets.filterIsInstance<IPingResponse>().take(expectedCount).toList().count()
            )
            transport.suspendClose()
            disconnect(transport)
            println("ping response done")
        }
    }

    @Test
    fun ultraAsyncTestSingleThreaded() {
        runBlocking(singleThreadScope.coroutineContext) {
            repeat(runCount) {
                delay(runCount.toLong())
                println("launching scope req")
                launch {
                    println("ultra async ping request st $it / $runCount")
                    try {
                        println("ping req impl")
                        pingRequestImpl(singleThreadScope, singleThreadProvider)
                        println("ping req impl done")
                    } catch (e: Throwable) {
                        println("error from ultraAsyncTestSingleThreaded.pingRequestImpl $it")
                        e.printStackTrace()
                        throw e
                    }
                    pingRequestImpl(singleThreadScope, singleThreadProvider)
                }
                println("delayed")
                delay(runCount.toLong())
                println("launching scope resp")
                launch {
                    println("ultra async ping response st $it / $runCount")
                    try {
                        pingResponseImpl(singleThreadScope, singleThreadProvider)
                    } catch (e: Throwable) {
                        println("error from ultraAsyncTestSingleThreaded.pingResponseImpl $it")
                        e.printStackTrace()
                        throw e
                    }
                    pingResponseImpl(singleThreadScope, singleThreadProvider)
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
        singleThreadExecutor = Executors.newSingleThreadExecutor()
        singleThreadScope = CoroutineScope(singleThreadExecutor.asCoroutineDispatcher())
        singleThreadProvider = AsynchronousChannelGroup.withThreadPool(singleThreadExecutor)!!
    }

    @After
    fun close() {
        singleThreadScope.cancel()
        singleThreadProvider.shutdownNow()
        println("delay")
        runBlocking { delay(10) }
        println("delay done")
        assertTrue(singleThreadProvider.awaitTermination(timeoutOffsetMs.toLong(), TimeUnit.MILLISECONDS))
        assertTrue(singleThreadExecutor.awaitTermination(timeoutOffsetMs.toLong(), TimeUnit.MILLISECONDS))

        val executorTasks = singleThreadExecutor.shutdownNow()
        executorTasks.forEach { println("leftover task $it") }
        assertTrue(executorTasks.isNullOrEmpty())
    }
}
