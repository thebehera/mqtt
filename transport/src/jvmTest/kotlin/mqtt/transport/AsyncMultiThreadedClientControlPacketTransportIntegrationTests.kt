package mqtt.transport

import org.junit.After
import org.junit.Before
import org.junit.Test
import java.util.concurrent.Executors
import kotlin.time.ExperimentalTime

@ExperimentalTime
class AsyncMultiThreadedClientControlPacketTransportIntegrationTests {
    private val timeoutOffsetMs = 150
    private val keepAliveTimeoutSeconds = 2
    private val integrationTestTimeoutMs = keepAliveTimeoutSeconds * 1000 + timeoutOffsetMs + 1
    val port = 60000
    private lateinit var asyncClientTestHarness: AsyncClientTestHarness
    val processors = Runtime.getRuntime().availableProcessors()
    val runCount = Math.min(1000, processors * processors)

    @Before
    fun connect() {
        val executorService = Executors.newFixedThreadPool(processors * 2)
        asyncClientTestHarness = asyncClientTestHarness(
            null, port, runCount, listOf(executorService), timeoutOffsetMs.toLong(),
            keepAliveTimeoutSeconds, integrationTestTimeoutMs.toLong()
        )
    }

    @Test(timeout = 6000)
    fun pingRequest() {
        asyncClientTestHarness.pingRequest()
    }

    @Test(timeout = 6000)
    fun pingResponse() {
        asyncClientTestHarness.pingResponse()
    }

    @Test
    fun ultraAsync() {
        asyncClientTestHarness.ultraAsync()
    }

    @After
    fun close() {
        asyncClientTestHarness.close()
    }
}