package mqtt.transport

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import mqtt.transport.nio.socket.NioClientSocket
import mqtt.transport.nio2.socket.AsyncClientSocket
import mqtt.transport.nio2.socket.AsyncServerSocket
import kotlin.time.Duration
import kotlin.time.ExperimentalTime

@ExperimentalCoroutinesApi
@ExperimentalTime
@ExperimentalUnsignedTypes
actual fun asyncClientSocket(
    coroutineScope: CoroutineScope,
    readTimeout: Duration,
    writeTimeout: Duration,
    bufferPool: BufferPool
): ClientToServerSocket {
    return AsyncClientSocket(
        coroutineScope,
        bufferPool,
        readTimeout,
        writeTimeout
    )
}

@ExperimentalCoroutinesApi
@ExperimentalTime
@ExperimentalUnsignedTypes
actual fun clientSocket(
    coroutineScope: CoroutineScope,
    blocking: Boolean,
    readTimeout: Duration,
    writeTimeout: Duration,
    bufferPool: BufferPool
): ClientToServerSocket {
    return NioClientSocket(
        coroutineScope,
        bufferPool,
        blocking,
        readTimeout,
        writeTimeout
    )
}

@ExperimentalCoroutinesApi
@ExperimentalUnsignedTypes
@ExperimentalTime
actual fun asyncServerSocket(
    coroutineScope: CoroutineScope,
    readTimeout: Duration,
    writeTimeout: Duration,
    bufferPool: BufferPool
): ServerToClientSocket {
    return AsyncServerSocket(
        coroutineScope,
        bufferPool,
        readTimeout,
        writeTimeout
    )
}