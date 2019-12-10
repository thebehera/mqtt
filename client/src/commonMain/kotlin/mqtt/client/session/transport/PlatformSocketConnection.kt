package mqtt.client.session.transport

import io.ktor.util.KtorExperimentalAPI
import mqtt.connection.IRemoteHost
import kotlin.coroutines.CoroutineContext

@KtorExperimentalAPI
expect class PlatformSocketConnection(
    remoteHost: IRemoteHost,
    ctx: CoroutineContext
) : SocketTransport
