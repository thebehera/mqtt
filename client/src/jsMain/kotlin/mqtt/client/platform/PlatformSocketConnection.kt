package mqtt.client.platform

import io.ktor.util.KtorExperimentalAPI
import mqtt.client.connection.ConnectionParameters
import mqtt.client.transport.SocketTransport
import kotlin.coroutines.CoroutineContext

@KtorExperimentalAPI
actual class PlatformSocketConnection actual constructor(
    override val parameters: ConnectionParameters,
    ctx: CoroutineContext
) : SocketTransport(ctx) {

    override val supportsNativeSockets = false

    override suspend fun buildNativeSocket() =
        throw UnsupportedOperationException("Native sockets are not supported on JS yet")
}