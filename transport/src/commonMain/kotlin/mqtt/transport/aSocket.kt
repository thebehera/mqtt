package mqtt.transport

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
    fun isOpen(): Boolean
    fun port(): UShort?
}

@ExperimentalUnsignedTypes
@ExperimentalTime
expect fun asyncClientSocket(): ClientToServerSocket


@ExperimentalUnsignedTypes
@ExperimentalTime
expect fun asyncServerSocket(): ServerSocket

@ExperimentalUnsignedTypes
@ExperimentalTime
expect fun clientSocket(blocking: Boolean): ClientToServerSocket

data class SocketOptions(
    val tcpNoDelay: Boolean? = null,
    val reuseAddress: Boolean? = null,
    val keepAlive: Boolean? = null,
    val receiveBuffer: UInt? = null,
    val sendBuffer: UInt? = null
)