package mqtt.client

import kotlinx.coroutines.CoroutineScope
import mqtt.buffer.BufferPool
import mqtt.connection.IRemoteHost
import mqtt.socket.isNodeJs

actual suspend fun loadCustomWebsocketImplementation(
    scope: CoroutineScope,
    pool: BufferPool,
    remoteHost: IRemoteHost
): ISocketController? = if (isNodeJs) {
    null
} else {
    val controller = BrowserWebsocketController(scope, pool, remoteHost)
    controller.connect()
    controller
}

actual fun getNetworkCapabilities() = if (isNodeJs) {
    NetworkCapabilities.FULL_SOCKET_ACCESS
} else {
    NetworkCapabilities.WEBSOCKETS_ONLY
}