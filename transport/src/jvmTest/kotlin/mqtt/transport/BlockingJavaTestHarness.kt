package mqtt.transport

import java.io.Closeable
import kotlin.time.ExperimentalTime

@ExperimentalTime
actual fun blockingClientTestHarness(
    host: String?,
    port: Int?,
    runCount: Int,
    platformExtras: List<Any>,
    timeoutOffsetMs: Long,
    keepAliveTimeoutSeconds: Int,
    integrationTestTimeoutMs: Long
): BlockingTestHarness =
    BlockingJavaTestHarness(port, host, runCount, timeoutOffsetMs, keepAliveTimeoutSeconds, integrationTestTimeoutMs)

@ExperimentalTime
class BlockingJavaTestHarness(
    port: Int? = null,
    host: String?,
    runCount: Int,
    timeoutOffsetMs: Long = 150,
    keepAliveTimeoutSeconds: Int = 2,
    integrationTestTimeoutMs: Long = keepAliveTimeoutSeconds * 1000 + timeoutOffsetMs + 1
) : BlockingTestHarness(runCount, timeoutOffsetMs, keepAliveTimeoutSeconds, integrationTestTimeoutMs), Closeable