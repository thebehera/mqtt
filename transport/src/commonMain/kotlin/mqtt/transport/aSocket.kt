package mqtt.transport

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlin.time.Duration
import kotlin.time.ExperimentalTime

interface SuspendCloseable {
    suspend fun close()
}

@ExperimentalTime
@ExperimentalUnsignedTypes
interface ClientSocket : SuspendCloseable {
    val scope: CoroutineScope
    val pool: BufferPool
    var readTimeout: Duration // can be changed during the course of the connection
    var writeTimeout: Duration
    val incoming: Flow<IncomingMessage>
    fun isOpen(): Boolean
    fun localPort(): UShort?
    fun remotePort(): UShort?
    suspend fun send(buffer: PlatformBuffer)
    var tag: Any?
}

@ExperimentalTime
@ExperimentalUnsignedTypes
interface ClientToServerSocket : ClientSocket {
    suspend fun open(hostname: String? = null, port: UShort)
}

@ExperimentalTime
@ExperimentalUnsignedTypes
interface ServerToClientSocket : SuspendCloseable {
    val scope: CoroutineScope
    fun port(): UShort?
    suspend fun bind(port: UShort? = null, host: String? = null)
    suspend fun listen(): Flow<ClientSocket>
    val connections: Map<UShort, ClientSocket>
    fun getStats(): List<String>

    suspend fun closeClient(port: UShort)
}


@ExperimentalCoroutinesApi
@ExperimentalUnsignedTypes
@ExperimentalTime
expect fun asyncClientSocket(
    coroutineScope: CoroutineScope,
    readTimeout: Duration,
    writeTimeout: Duration,
    bufferPool: BufferPool
): ClientToServerSocket


@ExperimentalCoroutinesApi
@ExperimentalUnsignedTypes
@ExperimentalTime
expect fun asyncServerSocket(
    coroutineScope: CoroutineScope,
    readTimeout: Duration,
    writeTimeout: Duration,
    bufferPool: BufferPool
): ServerToClientSocket

@ExperimentalUnsignedTypes
@ExperimentalTime
@ExperimentalCoroutinesApi
expect fun clientSocket(
    coroutineScope: CoroutineScope,
    blocking: Boolean,
    readTimeout: Duration,
    writeTimeout: Duration,
    bufferPool: BufferPool
): ClientToServerSocket