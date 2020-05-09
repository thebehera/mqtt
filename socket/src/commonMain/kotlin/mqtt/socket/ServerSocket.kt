@file:Suppress("EXPERIMENTAL_API_USAGE")

package mqtt.socket

import mqtt.buffer.SuspendCloseable
import kotlin.time.ExperimentalTime


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
expect fun asyncServerSocket(): ServerSocket

expect fun readStats(port: UShort, contains: String): List<String>