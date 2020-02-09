package mqtt.transport

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import mqtt.transport.nio2.socket.AsyncClientSocket
import mqtt.transport.nio2.socket.AsyncServerSocket
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
    readTimeout: Duration,
    writeTimeout: Duration
): ServerToClientSocket<*> {
    return AsyncServerSocket(
        coroutineScope,
        ByteBufferPool,
        readTimeout,
        writeTimeout
    )
}