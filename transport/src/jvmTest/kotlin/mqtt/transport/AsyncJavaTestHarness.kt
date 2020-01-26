package mqtt.transport

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.asCoroutineDispatcher
import java.io.Closeable
import java.nio.channels.AsynchronousChannelGroup
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import kotlin.test.assertTrue
import kotlin.time.ExperimentalTime


@ExperimentalTime
actual fun asyncClientTestHarness(
    host: String?,
    port: Int?,
    runCount: Int,
    platformExtras: List<Any>,
    timeoutOffsetMs: Long,
    keepAliveTimeoutSeconds: Int,
    integrationTestTimeoutMs: Long
): AsyncClientTestHarness {
    val executorService = platformExtras.filterIsInstance<ExecutorService>().firstOrNull()
        ?: Executors.newSingleThreadExecutor()
    return AsyncJavaTestHarness(
        executorService,
        port,
        host,
        runCount,
        timeoutOffsetMs,
        keepAliveTimeoutSeconds,
        integrationTestTimeoutMs
    )
}

@ExperimentalTime
class AsyncJavaTestHarness(
    private val executorService: ExecutorService,
    port: Int? = null,
    host: String?,
    runCount: Int,
    timeoutOffsetMs: Long = 150,
    keepAliveTimeoutSeconds: Int = 2,
    integrationTestTimeoutMs: Long = keepAliveTimeoutSeconds * 1000 + timeoutOffsetMs + 1
) : AsyncClientTestHarness(runCount, timeoutOffsetMs, keepAliveTimeoutSeconds, integrationTestTimeoutMs), Closeable {

    override val scope = CoroutineScope(executorService.asCoroutineDispatcher())
    override val channelGroup = AsynchronousChannelGroup.withThreadPool(executorService)!!


    override fun close() {
        channelGroup.shutdownNow()
        assertTrue(channelGroup.awaitTermination(timeoutOffsetMs, TimeUnit.MILLISECONDS))
        assertTrue(executorService.awaitTermination(timeoutOffsetMs, TimeUnit.MILLISECONDS))
        val executorTasks = executorService.shutdownNow()
        executorTasks.forEach { println("leftover task $it") }
        assertTrue(executorTasks.isNullOrEmpty())
    }
}