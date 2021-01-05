package mqtt.client

import kotlinx.coroutines.CoroutineScope
import mqtt.buffer.BufferPool
import mqtt.connection.IConnectionOptions
import mqtt.persistence.isNodeJs

actual suspend fun loadCustomWebsocketImplementation(
    scope: CoroutineScope,
    pool: BufferPool,
    connectionOptions: IConnectionOptions
): ISocketController? {
    return if (isNodeJs()) {
        null
    } else {
        val controller = BrowserWebsocketController(scope, pool, connectionOptions)
        controller.connect()
        controller
    }
}

actual fun getNetworkCapabilities() = if (isNodeJs()) {
    NetworkCapabilities.FULL_SOCKET_ACCESS
} else {
    NetworkCapabilities.WEBSOCKETS_ONLY
}