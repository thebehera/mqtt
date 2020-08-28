@file:Suppress("EXPERIMENTAL_API_USAGE")

package mqtt.socket

import mqtt.buffer.BufferPool
import mqtt.buffer.PlatformBuffer
import mqtt.buffer.SuspendCloseable
import kotlin.time.Duration
import kotlin.time.ExperimentalTime
import kotlin.time.seconds

@ExperimentalTime
interface ClientSocket : SuspendCloseable {
    fun isOpen(): Boolean
    fun localPort(): UShort?
    fun remotePort(): UShort?
    suspend fun <T> read(timeout: Duration = 1.seconds, bufferRead: (PlatformBuffer, Int) -> T): SocketDataRead<T>
    suspend fun <T> readTyped(timeout: Duration = 1.seconds, bufferRead: (PlatformBuffer) -> T) =
        read(timeout) { buffer, _ ->
            bufferRead(buffer)
        }.result

    suspend fun write(buffer: PlatformBuffer, timeout: Duration = 1.seconds): Int
}

data class SocketDataRead<T>(val result: T, val bytesRead: Int)

@ExperimentalTime
fun getClientSocket(pool: BufferPool = BufferPool(), ssl: Boolean = false): ClientToServerSocket {
    try {
        return asyncClientSocket(pool, ssl)
    } catch (e: Throwable) {
        // failed to allocate async socket channel based socket, fallback to nio
    }
    return clientSocket(false, pool, ssl)
}

@ExperimentalTime
expect fun asyncClientSocket(pool: BufferPool = BufferPool(), ssl: Boolean = true): ClientToServerSocket

@ExperimentalTime
expect fun clientSocket(blocking: Boolean = false, pool: BufferPool = BufferPool(), ssl: Boolean = false): ClientToServerSocket

//@ExperimentalTime
//expect fun asyncClientSocket(pool: BufferPool): ClientToServerSocket