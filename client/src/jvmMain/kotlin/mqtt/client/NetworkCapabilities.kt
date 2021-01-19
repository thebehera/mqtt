package mqtt.client

import kotlinx.coroutines.CoroutineScope
import mqtt.client.socket.ISocketController
import mqtt.connection.IConnectionOptions

actual suspend fun loadCustomWebsocketImplementation(
    scope: CoroutineScope,
    connectionOptions: IConnectionOptions
): ISocketController? = null

actual fun getNetworkCapabilities() = NetworkCapabilities.FULL_SOCKET_ACCESS