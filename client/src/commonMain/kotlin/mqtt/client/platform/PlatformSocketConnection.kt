package mqtt.client.platform

import io.ktor.util.KtorExperimentalAPI
import mqtt.client.transport.SocketTransport
import mqtt.connection.IRemoteHost
import kotlin.coroutines.CoroutineContext

@KtorExperimentalAPI
expect class PlatformSocketConnection(
    remoteHost: IRemoteHost,
    ctx: CoroutineContext
) : SocketTransport
