package mqtt.socket

import mqtt.socket.nio.NioClientSocket
import mqtt.socket.nio2.AsyncClientSocket
import mqtt.socket.nio2.AsyncServerSocket
import kotlin.time.ExperimentalTime

@ExperimentalTime
@ExperimentalUnsignedTypes
actual fun asyncClientSocket(): ClientToServerSocket? = AsyncClientSocket()

@ExperimentalTime
@ExperimentalUnsignedTypes
actual fun clientSocket(blocking: Boolean): ClientToServerSocket? =
    NioClientSocket(blocking)

@ExperimentalUnsignedTypes
@ExperimentalTime
actual fun asyncServerSocket(): ServerSocket? = AsyncServerSocket()
