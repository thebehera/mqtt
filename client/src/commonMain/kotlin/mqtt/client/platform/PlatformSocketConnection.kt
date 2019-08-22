package mqtt.client.platform

import io.ktor.util.KtorExperimentalAPI
import mqtt.client.transport.SocketTransport
import mqtt.connection.IMqttConfiguration
import kotlin.coroutines.CoroutineContext

@KtorExperimentalAPI
expect class PlatformSocketConnection(
    configuration: IMqttConfiguration,
    ctx: CoroutineContext
) : SocketTransport
