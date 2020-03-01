package mqtt.transport

import kotlinx.coroutines.ExperimentalCoroutinesApi
import mqtt.transport.nio.socket.NioClientSocket
import mqtt.transport.nio2.socket.AsyncClientSocket
import mqtt.transport.nio2.socket.AsyncServerSocket
import kotlin.time.ExperimentalTime

@ExperimentalCoroutinesApi
@ExperimentalTime
@ExperimentalUnsignedTypes
actual fun asyncClientSocket(bufferPool: BufferPool): ClientToServerSocket = AsyncClientSocket(bufferPool)

@ExperimentalCoroutinesApi
@ExperimentalTime
@ExperimentalUnsignedTypes
actual fun clientSocket(blocking: Boolean, bufferPool: BufferPool): ClientToServerSocket =
    NioClientSocket(bufferPool, blocking)

@ExperimentalCoroutinesApi
@ExperimentalUnsignedTypes
@ExperimentalTime
actual fun asyncServerSocket(bufferPool: BufferPool): ServerSocket = AsyncServerSocket(bufferPool)