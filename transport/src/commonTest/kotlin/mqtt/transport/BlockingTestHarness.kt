package mqtt.transport

import kotlinx.coroutines.CoroutineScope
import mqtt.connection.ClientControlPacketTransport
import mqtt.wire.control.packet.IConnectionRequest
import kotlin.coroutines.EmptyCoroutineContext
import kotlin.time.ExperimentalTime
import kotlin.time.seconds

@ExperimentalTime
expect fun blockingClientTestHarness(
    host: String?,
    port: Int? = null,
    runCount: Int,
    platformExtras: List<Any>,
    timeoutOffsetMs: Long = 150,
    keepAliveTimeoutSeconds: Int = 2,
    integrationTestTimeoutMs: Long = keepAliveTimeoutSeconds * 1000 + timeoutOffsetMs + 1
): BlockingTestHarness


@ExperimentalTime
abstract class BlockingTestHarness(
    runCount: Int,
    timeoutOffsetMs: Long = 150,
    keepAliveTimeoutSeconds: Int = 2,
    integrationTestTimeoutMs: Long = keepAliveTimeoutSeconds * 1000 + timeoutOffsetMs + 1
) : TestHarness(runCount, timeoutOffsetMs, keepAliveTimeoutSeconds, integrationTestTimeoutMs) {

    override suspend fun createClient(connectionRequest: IConnectionRequest) =
        blockingMqttClient(scope, connectionRequest, 12000, 1.seconds)

    override val scope: CoroutineScope = CoroutineScope(EmptyCoroutineContext)
    override fun pingRequest() {
        repeat(runCount) {
            println("ping request $it / $runCount")
            try {
                blockWithTimeout(keepAliveTimeoutSeconds * 2000L) {
                    pingRequestImpl()
                }
            } catch (e: Throwable) {
                println("error from pingRequest $it $e")
                throw e
            }
        }
    }

    override fun pingResponse() {
        repeat(runCount) {
            println("ping response st $it / $runCount")
            try {
                blockWithTimeout(keepAliveTimeoutSeconds * 2000L) {
                    pingResponseImpl()
                }
            } catch (e: Throwable) {
                println("error from pingResponse $it $e")
                throw e
            }
        }
    }

    override fun disconnect(transport: ClientControlPacketTransport) {
        transport.close()
        super.disconnect(transport)
    }

}