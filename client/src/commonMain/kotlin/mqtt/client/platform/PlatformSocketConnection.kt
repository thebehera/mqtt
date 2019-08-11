package mqtt.client.platform

import io.ktor.util.KtorExperimentalAPI
import mqtt.client.connection.parameters.IMqttConfiguration
import mqtt.client.transport.SocketTransport
import kotlin.coroutines.CoroutineContext

@KtorExperimentalAPI
expect class PlatformSocketConnection(
    configuration: IMqttConfiguration,
    ctx: CoroutineContext
) : SocketTransport