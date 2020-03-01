package mqtt.transport

import kotlinx.coroutines.ExperimentalCoroutinesApi
import mqtt.transport.nio.socket.NioClientSocket
import mqtt.transport.nio2.socket.AsyncClientSocket
import mqtt.transport.nio2.socket.AsyncServerSocket
import kotlin.time.ExperimentalTime

@ExperimentalCoroutinesApi
@ExperimentalTime
@ExperimentalUnsignedTypes
actual fun asyncClientSocket(): ClientToServerSocket = AsyncClientSocket()

@ExperimentalCoroutinesApi
@ExperimentalTime
@ExperimentalUnsignedTypes
actual fun clientSocket(blocking: Boolean): ClientToServerSocket =
    NioClientSocket(blocking)

@ExperimentalCoroutinesApi
@ExperimentalUnsignedTypes
@ExperimentalTime
actual fun asyncServerSocket(): ServerSocket = AsyncServerSocket()