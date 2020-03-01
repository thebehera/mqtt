package mqtt.transport

import mqtt.transport.nio.socket.JvmServer
import mqtt.transport.nio.socket.NioClientSocket
import mqtt.transport.nio2.socket.AsyncClientSocket
import mqtt.transport.nio2.socket.AsyncServerSocket
import kotlin.time.ExperimentalTime

@ExperimentalTime
@ExperimentalUnsignedTypes
actual fun asyncClientSocket(): ClientToServerSocket = AsyncClientSocket()

@ExperimentalTime
@ExperimentalUnsignedTypes
actual fun clientSocket(blocking: Boolean): ClientToServerSocket =
    NioClientSocket(blocking)

@ExperimentalUnsignedTypes
@ExperimentalTime
actual fun asyncServerSocket(): ServerSocket = AsyncServerSocket()


@ExperimentalUnsignedTypes
@ExperimentalTime
actual fun server(serverSocket: ServerSocket): Server = JvmServer(serverSocket)