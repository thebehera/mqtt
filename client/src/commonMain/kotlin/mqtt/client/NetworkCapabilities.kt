package mqtt.client

import kotlinx.coroutines.CoroutineScope
import mqtt.connection.IConnectionOptions

enum class NetworkCapabilities {
    FULL_SOCKET_ACCESS,
    WEBSOCKETS_ONLY
}

expect fun getNetworkCapabilities(): NetworkCapabilities


expect suspend fun loadCustomWebsocketImplementation(
    scope: CoroutineScope,
    connectionOptions: IConnectionOptions
): ISocketController?

