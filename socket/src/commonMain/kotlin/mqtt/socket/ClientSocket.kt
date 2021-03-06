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
    val pool: BufferPool

    fun isOpen(): Boolean
    fun localPort(): UShort?
    fun remotePort(): UShort?
    suspend fun read(buffer: PlatformBuffer, timeout: Duration): Int
    suspend fun read(timeout: Duration = 1.seconds) = read(timeout) { buffer, bytesRead -> buffer.readUtf8(bytesRead) }
    suspend fun <T> read(timeout: Duration = 1.seconds, bufferRead: (PlatformBuffer, Int) -> T): SocketDataRead<T> {
        var bytesRead = 0
        val result = pool.borrowSuspend {
            bytesRead = read(it, timeout)
            bufferRead(it, bytesRead)
        }
        return SocketDataRead(result, bytesRead)
    }
    suspend fun <T> readTyped(timeout: Duration = 1.seconds, bufferRead: (PlatformBuffer) -> T) =
        read(timeout) { buffer, _ ->
            bufferRead(buffer)
        }.result

    suspend fun write(buffer: PlatformBuffer, timeout: Duration = 1.seconds): Int
    suspend fun write(buffer: String, timeout: Duration = 1.seconds): Int
            = write(buffer.toBuffer().also { it.position(it.limit().toInt()) }, timeout)

    suspend fun writeFully(buffer: PlatformBuffer, timeout: Duration) {
        while (buffer.position() < buffer.limit()) {
            write(buffer, timeout)
        }
    }
}

data class SocketDataRead<T>(val result: T, val bytesRead: Int)

@ExperimentalTime
suspend fun openClientSocket(port: UShort,
                             timeout: Duration = 1.seconds,
                             hostname: String? = null,
                             socketOptions: SocketOptions? = null): ClientToServerSocket {
    val socket = getClientSocket()
    socket.open(port, timeout, hostname, socketOptions)
    return socket
}

@ExperimentalTime
fun getClientSocket(pool: BufferPool = BufferPool()): ClientToServerSocket {
    try {
        return asyncClientSocket(pool)
    } catch (e: Throwable) {
        // failed to allocate async socket channel based socket, fallback to nio
    }
    return clientSocket(false, pool)
}

@ExperimentalTime
expect fun asyncClientSocket(pool: BufferPool = BufferPool()): ClientToServerSocket

@ExperimentalTime
expect fun clientSocket(blocking: Boolean = false, pool: BufferPool = BufferPool()): ClientToServerSocket

