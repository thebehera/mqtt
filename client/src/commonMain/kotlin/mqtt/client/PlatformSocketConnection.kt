package mqtt.client

import kotlin.coroutines.CoroutineContext

expect class PlatformSocketConnection(parameters: ConnectionParameters,
                                      ctx: CoroutineContext) : SocketConnection