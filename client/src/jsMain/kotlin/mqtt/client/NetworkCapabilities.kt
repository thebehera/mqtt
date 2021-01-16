package mqtt.client

import kotlinx.coroutines.CoroutineScope
import mqtt.connection.IConnectionOptions
import mqtt.persistence.isNodeJs

actual suspend fun loadCustomWebsocketImplementation(
    scope: CoroutineScope,
    connectionOptions: IConnectionOptions
): ISocketController? {
    return if (isNodeJs()) {
        null
    } else {
        val controller = BrowserWebsocketController(scope, connectionOptions)
        controller.connect()
        controller
    }
}

actual fun getNetworkCapabilities() = if (isNodeJs()) {
    NetworkCapabilities.FULL_SOCKET_ACCESS
} else {
    NetworkCapabilities.WEBSOCKETS_ONLY
}