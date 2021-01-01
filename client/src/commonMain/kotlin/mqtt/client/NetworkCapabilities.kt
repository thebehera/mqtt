package mqtt.client

import kotlinx.coroutines.CoroutineScope
import mqtt.buffer.BufferPool
import mqtt.connection.IRemoteHost

enum class NetworkCapabilities {
    FULL_SOCKET_ACCESS,
    WEBSOCKETS_ONLY
}

expect fun getNetworkCapabilities(): NetworkCapabilities


expect suspend fun loadCustomWebsocketImplementation(
    scope: CoroutineScope,
    pool: BufferPool,
    remoteHost: IRemoteHost
): ISocketController?

