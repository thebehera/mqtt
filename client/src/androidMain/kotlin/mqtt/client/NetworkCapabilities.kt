package mqtt.client

import kotlinx.coroutines.CoroutineScope
import mqtt.buffer.BufferPool
import mqtt.connection.IRemoteHost

actual suspend fun loadCustomWebsocketImplementation(
    scope: CoroutineScope,
    pool: BufferPool,
    remoteHost: IRemoteHost
): ISocketController? = null

actual fun getNetworkCapabilities() = NetworkCapabilities.FULL_SOCKET_ACCESS