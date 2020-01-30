package mqtt.transport

import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.time.ExperimentalTime

@ExperimentalTime
class BlockingClientControlPacketTransportIntegrationTests {
    private val timeoutOffsetMs = 150
    private val keepAliveTimeoutSeconds = 1
    private val integrationTestTimeoutMs = keepAliveTimeoutSeconds * 1000 + timeoutOffsetMs + 1
    val runCount = 1
    val port = 60000
    private lateinit var blockingClientTestHarness: BlockingTestHarness

    @BeforeTest
    fun connect() {
        blockingClientTestHarness = blockingClientTestHarness(
            null, port, runCount, emptyList(), timeoutOffsetMs.toLong(),
            keepAliveTimeoutSeconds, integrationTestTimeoutMs.toLong()
        )
    }

    @Test
    fun pingRequest() {
        blockWithTimeout(5000) {
            blockingClientTestHarness.pingRequest()
        }
    }

    @Test
    fun pingResponse() = blockWithTimeout(6000) {
        blockingClientTestHarness.pingResponse()
    }

    @Test
    fun ultraAsync() = blockWithTimeout(15000) {
        blockingClientTestHarness.ultraAsync()
    }

    @AfterTest
    fun close() {
        blockingClientTestHarness.close()
    }
}