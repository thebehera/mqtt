package mqtt.client.platform

import mqtt.client.connection.ConnectionParameters
import mqtt.client.transport.SocketTransport
import kotlin.coroutines.CoroutineContext

expect class PlatformSocketConnection(parameters: ConnectionParameters,
                                      ctx: CoroutineContext) : SocketTransport