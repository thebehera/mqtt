package mqtt.client.session.transport.nio


import kotlinx.coroutines.*
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
    private val integrationTestTimeoutMs = keepAliveTimeoutSeconds * 1000 + timeoutOffsetMs + 1

    val processors = Runtime.getRuntime().availableProcessors()
    val runCount = processors * 3

    val singleThreadExecutor = Executors.newSingleThreadExecutor()
    val singleThreadScope = CoroutineScope(singleThreadExecutor.asCoroutineDispatcher())
    val singleThreadProvider = AsynchronousChannelGroup.withThreadPool(singleThreadExecutor)!!

    val multiThreadExecutor = Executors.newFixedThreadPool(runCount * 2)
    val multiThreadScope = CoroutineScope(multiThreadExecutor.asCoroutineDispatcher())
    val multiThreadProvider = AsynchronousChannelGroup.withThreadPool(multiThreadExecutor)

    fun connect(scope: CoroutineScope, channelGroup: AsynchronousChannelGroup? = null): ClientControlPacketTransport {
        val connectionRequest = ConnectionRequest(
            clientId = "test${Random.nextInt()}",
            keepAliveSeconds = keepAliveTimeoutSeconds.toUShort()
        )
        assert(integrationTestTimeoutMs > connectionRequest.keepAliveTimeoutSeconds.toInt() * 1000 + timeoutOffsetMs) { "Integration timeout too low" }
        var transport: ClientControlPacketTransport? = null
        scope.blockWithTimeout(timeoutOffsetMs.toLong()) {
            val t = asyncClientTransport(scope, connectionRequest, channelGroup)
            assert(t.open(60_000.toUShort()).isSuccessful) { "incorrect connack message" }
            transport = t
        }
        assertNotNull(transport!!.assignedPort())
        return transport!!
    }

    @Test
    fun pingRequestSingleThread() {
        repeat(runCount) {
            println("ping request st $it / $runCount")
            try {
                pingRequestImpl(singleThreadScope, singleThreadProvider)
            } catch (e: Throwable) {
                println("error from pingRequestMultiThread $it")
                e.printStackTrace()
                throw e
            }
        }
    }

    @Test
    fun pingRequestMultiThread() {
        repeat(runCount) {
            println("ping request mt $it / $runCount")
            try {
                pingRequestImpl(multiThreadScope, multiThreadProvider)
            } catch (e: Throwable) {
                println("error from pingRequestMultiThread $it")
                e.printStackTrace()
                throw e
            }
        }
    }

    fun pingRequestImpl(scope: CoroutineScope, channelGroup: AsynchronousChannelGroup? = null) {
        val transport = connect(scope, channelGroup)
        scope.blockWithTimeout(integrationTestTimeoutMs.toLong() + timeoutOffsetMs) {
            val completedWriteChannel = Channel<ControlPacket>()
            transport.completedWrite = completedWriteChannel
            val expectedCount = max(
                1,
                integrationTestTimeoutMs / (transport.connectionRequest.keepAliveTimeoutSeconds.toInt() * 1000)
            )
            val responses =
                completedWriteChannel.consumeAsFlow().filterIsInstance<IPingRequest>().take(expectedCount).toList()
            assertEquals(expectedCount, responses.count())
            transport.suspendClose()
        }
        disconnect(transport)
    }

    fun pingResponseImpl(scope: CoroutineScope, channelGroup: AsynchronousChannelGroup? = null) {
        val transport = connect(scope, channelGroup)
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

    @Test
    fun pingResponseSingleThread() {
        repeat(runCount) {
            println("ping response st $it / $runCount")
            try {
                pingResponseImpl(singleThreadScope, singleThreadProvider)
            } catch (e: Throwable) {
                println("error from pingResponseSingleThread $it")
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
                pingResponseImpl(multiThreadScope, multiThreadProvider)
            } catch (e: Throwable) {
                println("error from pingResponseMultiThreaded $it")
                e.printStackTrace()
                throw e
            }
        }
    }

    @Test
    fun ultraAsyncTestSingleThreaded() {
        runBlocking {
            repeat(runCount) {
                delay(runCount * 50.toLong())
                launch {
                    println("ultra async ping request st $it / $runCount")
                    try {
                        pingRequestImpl(multiThreadScope, multiThreadProvider)
                    } catch (e: Throwable) {
                        println("error from ultraAsyncTestSingleThreaded.pingRequestImpl $it")
                        e.printStackTrace()
                        throw e
                    }
                    pingRequestImpl(singleThreadScope, singleThreadProvider)
                }
                delay(runCount * 50.toLong())
                launch {
                    println("ultra async ping response st $it / $runCount")
                    try {
                        pingResponseImpl(multiThreadScope, multiThreadProvider)
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

    @Test
    fun ultraAsyncTestMultiThreaded() {
        runBlocking {
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

}

