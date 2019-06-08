package mqtt.client.platform

import io.ktor.util.KtorExperimentalAPI
import mqtt.client.connection.ConnectionParameters
import mqtt.client.transport.SocketTransport
import kotlin.coroutines.CoroutineContext

@KtorExperimentalAPI
expect class PlatformSocketConnection(parameters: ConnectionParameters,
                                      ctx: CoroutineContext) : SocketTransport