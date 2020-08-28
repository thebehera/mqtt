package mqtt.socket

import mqtt.buffer.BufferPool
import mqtt.socket.nio.NioClientSocket
import mqtt.socket.nio2.AsyncClientSocket
import mqtt.socket.nio2.AsyncServerSocket
import kotlin.time.ExperimentalTime

@ExperimentalTime
@ExperimentalUnsignedTypes
actual fun asyncClientSocket(pool: BufferPool, ssl: Boolean): ClientToServerSocket = AsyncClientSocket(pool, ssl)

@ExperimentalTime
@ExperimentalUnsignedTypes
actual fun clientSocket(blocking: Boolean, pool: BufferPool, ssl: Boolean): ClientToServerSocket =
    NioClientSocket(blocking, pool)

@ExperimentalUnsignedTypes
@ExperimentalTime
actual fun asyncServerSocket(): ServerSocket = AsyncServerSocket()
