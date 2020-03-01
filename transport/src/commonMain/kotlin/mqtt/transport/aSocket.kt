package mqtt.transport

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
    val pool: BufferPool
    fun isOpen(): Boolean
    fun localPort(): UShort?
    fun remotePort(): UShort?
    var tag: Any?
}

@ExperimentalTime
@ExperimentalUnsignedTypes
interface ClientToServerSocket : ClientSocket {
    suspend fun open(
        timeout: Duration,
        port: UShort,
        hostname: String? = null,
        socketOptions: SocketOptions? = null
    ): SocketOptions
}

@ExperimentalTime
@ExperimentalUnsignedTypes
interface ServerSocket : SuspendCloseable {
    fun port(): UShort?
    suspend fun bind(port: UShort? = null, host: String? = null, socketOptions: SocketOptions? = null): SocketOptions
    suspend fun listen(): Flow<ClientSocket>
    val connections: Map<UShort, ClientSocket>
    fun getStats(): List<String>

    suspend fun closeClient(port: UShort)
}


@ExperimentalCoroutinesApi
@ExperimentalUnsignedTypes
@ExperimentalTime
expect fun asyncClientSocket(
    bufferPool: BufferPool
): ClientToServerSocket


@ExperimentalCoroutinesApi
@ExperimentalUnsignedTypes
@ExperimentalTime
expect fun asyncServerSocket(
    bufferPool: BufferPool
): ServerSocket

@ExperimentalUnsignedTypes
@ExperimentalTime
@ExperimentalCoroutinesApi
expect fun clientSocket(
    blocking: Boolean,
    bufferPool: BufferPool
): ClientToServerSocket


data class SocketOptions(
    val tcpNoDelay: Boolean? = null,
    val reuseAddress: Boolean? = null,
    val keepAlive: Boolean? = null,
    val receiveBuffer: UInt? = null,
    val sendBuffer: UInt? = null
)