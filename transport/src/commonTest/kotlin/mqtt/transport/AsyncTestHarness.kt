package mqtt.transport

import mqtt.wire.control.packet.IConnectionRequest
import kotlin.time.ExperimentalTime

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
    runCount: Int,
    timeoutOffsetMs: Long = 150,
    keepAliveTimeoutSeconds: Int = 2,
    integrationTestTimeoutMs: Long = keepAliveTimeoutSeconds * 1000 + timeoutOffsetMs + 1
) : TestHarness(runCount, timeoutOffsetMs, keepAliveTimeoutSeconds, integrationTestTimeoutMs) {

    override suspend fun createClient(connectionRequest: IConnectionRequest) =
        aMqttClient(scope, connectionRequest, 12000, channelGroup)

}