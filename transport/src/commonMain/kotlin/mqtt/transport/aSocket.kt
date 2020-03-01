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
    fun isOpen(): Boolean
    fun localPort(): UShort?
    fun remotePort(): UShort?
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
    suspend fun bind(
        port: UShort? = null,
        host: String? = null,
        socketOptions: SocketOptions? = null,
        backlog: UInt = 0.toUInt()
    ): SocketOptions

    suspend fun accept(): ClientSocket
    fun port(): UShort?

    // Should move this to the "Server" Interface
    suspend fun listen(): Flow<ClientSocket>

    val connections: Map<UShort, ClientSocket>
    suspend fun closeClient(port: UShort)
    fun getStats(): List<String> // only for debugging
}

@ExperimentalTime
interface Server {
    val connections: Map<UShort, ClientSocket>
    suspend fun listen(): Flow<ClientSocket>
    suspend fun closeClient(port: UShort)
    fun getStats(): List<String>
}


@ExperimentalCoroutinesApi
@ExperimentalUnsignedTypes
@ExperimentalTime
expect fun asyncClientSocket(): ClientToServerSocket


@ExperimentalCoroutinesApi
@ExperimentalUnsignedTypes
@ExperimentalTime
expect fun asyncServerSocket(): ServerSocket

@ExperimentalUnsignedTypes
@ExperimentalTime
@ExperimentalCoroutinesApi
expect fun clientSocket(blocking: Boolean): ClientToServerSocket


data class SocketOptions(
    val tcpNoDelay: Boolean? = null,
    val reuseAddress: Boolean? = null,
    val keepAlive: Boolean? = null,
    val receiveBuffer: UInt? = null,
    val sendBuffer: UInt? = null
)