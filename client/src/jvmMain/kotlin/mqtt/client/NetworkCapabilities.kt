package mqtt.client

import kotlinx.coroutines.CoroutineScope
import mqtt.buffer.BufferPool
import mqtt.connection.IConnectionOptions

actual suspend fun loadCustomWebsocketImplementation(
    scope: CoroutineScope,
    pool: BufferPool,
    connectionOptions: IConnectionOptions
): ISocketController? = null

actual fun getNetworkCapabilities() = NetworkCapabilities.FULL_SOCKET_ACCESS