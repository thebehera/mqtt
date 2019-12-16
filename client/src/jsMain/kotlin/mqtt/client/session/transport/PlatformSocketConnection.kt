package mqtt.client.session.transport

import io.ktor.util.KtorExperimentalAPI
import mqtt.connection.IRemoteHost
import kotlin.coroutines.CoroutineContext

@KtorExperimentalAPI
actual class PlatformSocketConnection actual constructor(
    override val remoteHost: IRemoteHost,
    ctx: CoroutineContext
) : SocketTransport(ctx) {
    override val supportsNativeSockets = false

    override suspend fun buildNativeSocket() =
        throw UnsupportedOperationException("Native sockets are not supported on JS yet")
}