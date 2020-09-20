@file:Suppress("EXPERIMENTAL_API_USAGE")

package mqtt.socket

import mqtt.buffer.BufferPool
import mqtt.buffer.PlatformBuffer
import mqtt.buffer.SuspendCloseable
import mqtt.buffer.toBuffer
import kotlin.time.Duration
import kotlin.time.ExperimentalTime
import kotlin.time.seconds

@ExperimentalTime
interface ClientSocket : SuspendCloseable {
    fun isOpen(): Boolean
    fun localPort(): UShort?
    fun remotePort(): UShort?
    suspend fun read(timeout: Duration = 1.seconds) = read(timeout) { buffer, bytesRead -> buffer.readUtf8(bytesRead) }
    suspend fun <T> read(timeout: Duration = 1.seconds, bufferRead: (PlatformBuffer, Int) -> T): SocketDataRead<T>
    suspend fun <T> readTyped(timeout: Duration = 1.seconds, bufferRead: (PlatformBuffer) -> T) =
        read(timeout) { buffer, _ ->
            bufferRead(buffer)
        }.result

    suspend fun write(buffer: PlatformBuffer, timeout: Duration = 1.seconds): Int
    suspend fun write(buffer: String, timeout: Duration = 1.seconds): Int = write(buffer.toBuffer(), timeout)

    suspend fun writeFully(buffer: PlatformBuffer, timeout: Duration) {
        while (buffer.position() < buffer.limit()) {
            write(buffer, timeout)
        }
    }
}

data class SocketDataRead<T>(val result: T, val bytesRead: Int)

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

