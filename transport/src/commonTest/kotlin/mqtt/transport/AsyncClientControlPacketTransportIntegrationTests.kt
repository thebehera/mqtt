package mqtt.transport

import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.time.ExperimentalTime

@ExperimentalTime
class AsyncClientControlPacketTransportIntegrationTests {
    private val timeoutOffsetMs = 150
    private val keepAliveTimeoutSeconds = 2
    private val integrationTestTimeoutMs = keepAliveTimeoutSeconds * 1000 + timeoutOffsetMs + 1
    val runCount = 10
    val port = 60000
    private lateinit var asyncClientTestHarness: AsyncClientTestHarness

    @BeforeTest
    fun connect() {
        asyncClientTestHarness = asyncClientTestHarness(
            null, port, runCount, emptyList(), timeoutOffsetMs.toLong(),
            keepAliveTimeoutSeconds, integrationTestTimeoutMs.toLong()
        )
    }

    @Test
    fun pingRequest() {
        blockWithTimeout(6000) {
            asyncClientTestHarness.pingRequest()
        }
    }

    @Test
    fun pingResponse() = blockWithTimeout(6000) {
        asyncClientTestHarness.pingResponse()
    }

    @Test
    fun ultraAsync() = blockWithTimeout(15000) {
        asyncClientTestHarness.ultraAsync()
    }

    @AfterTest
    fun close() {
        asyncClientTestHarness.close()
    }
}