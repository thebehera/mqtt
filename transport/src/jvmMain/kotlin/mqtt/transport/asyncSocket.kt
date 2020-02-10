package mqtt.transport

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import mqtt.transport.nio.socket.NioClientSocket
import mqtt.transport.nio2.socket.AsyncClientSocket
import mqtt.transport.nio2.socket.AsyncServerSocket
import mqtt.transport.nio2.socket.AsyncServerSocket2
import java.nio.ByteBuffer
import kotlin.time.Duration
import kotlin.time.ExperimentalTime

@ExperimentalCoroutinesApi
@ExperimentalTime
@ExperimentalUnsignedTypes
actual fun asyncClientSocket(
    coroutineScope: CoroutineScope,
    readTimeout: Duration,
    writeTimeout: Duration
): ClientToServerSocket<*> {
    return AsyncClientSocket(
        coroutineScope,
        ByteBufferPool,
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
    writeTimeout: Duration
): ClientToServerSocket<*> {
    return NioClientSocket(
        coroutineScope,
        ByteBufferPool,
        blocking,
        readTimeout,
        writeTimeout
    )
}

object ByteBufferPool : BufferPool<ByteBuffer> {
    override fun borrow(): ByteBuffer {
        return ByteBuffer.allocateDirect(8096)
    }
}

@ExperimentalCoroutinesApi
@ExperimentalUnsignedTypes
@ExperimentalTime
actual fun asyncServerSocket(
    coroutineScope: CoroutineScope,
    version: Int,
    readTimeout: Duration,
    writeTimeout: Duration
): ServerToClientSocket<*> {
    return if (version == 2) {
        AsyncServerSocket2(
            coroutineScope,
            ByteBufferPool,
            readTimeout,
            writeTimeout
        )
    } else {
        AsyncServerSocket(
            coroutineScope,
            ByteBufferPool,
            readTimeout,
            writeTimeout
        )
    }
}