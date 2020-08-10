@file:Suppress("EXPERIMENTAL_API_USAGE")

package mqtt.socket

import mqtt.buffer.BufferPool
import mqtt.buffer.PlatformBuffer
import mqtt.buffer.SuspendCloseable
import kotlin.time.Duration
import kotlin.time.ExperimentalTime

@ExperimentalTime
interface ClientSocket : SuspendCloseable {
    fun isOpen(): Boolean
    fun localPort(): UShort?
    fun remotePort(): UShort?
    suspend fun <T> read(timeout: Duration, bufferRead: (PlatformBuffer, Int) -> T): SocketDataRead<T>
    suspend fun <T> readTyped(timeout: Duration, bufferRead: (PlatformBuffer) -> T) = read(timeout) { buffer, _ ->
        bufferRead(buffer)
    }.result

    suspend fun read(timeout: Duration): SocketPlatformBufferRead

    suspend fun write(buffer: PlatformBuffer, timeout: Duration): Int
}

data class SocketDataRead<T>(val result: T, val bytesRead: Int)

/**
 * Buffer filled with data from the read call. When done with the buffer, use the RecycleCallback to release the buffer back into the pool
 */
data class SocketPlatformBufferRead(
    val bufferRead: PlatformBuffer,
    val bytesRead: Int,
    val recycleCallback: BufferPool.RecycleCallback
)

@ExperimentalTime
fun getClientSocket(): ClientToServerSocket {
    try {
        return asyncClientSocket()
    } catch (e: Throwable) {
        // failed to allocate async socket channel based socket, fallback to nio
    }
    return clientSocket(false)
}

@ExperimentalTime
expect fun asyncClientSocket(pool: BufferPool = BufferPool()): ClientToServerSocket

@ExperimentalTime
expect fun clientSocket(blocking: Boolean = false, pool: BufferPool = BufferPool()): ClientToServerSocket

