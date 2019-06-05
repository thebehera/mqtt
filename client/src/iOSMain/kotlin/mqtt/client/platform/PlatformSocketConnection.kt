package mqtt.client.platform

import mqtt.client.connection.ConnectionParameters
import mqtt.client.transport.SocketTransport
import kotlin.coroutines.CoroutineContext

// Default implementation is a no op for now until native sockets are wrapped by coroutines
actual class PlatformSocketConnection actual constructor(
    override val parameters: ConnectionParameters,
    ctx: CoroutineContext
) : SocketTransport(ctx)