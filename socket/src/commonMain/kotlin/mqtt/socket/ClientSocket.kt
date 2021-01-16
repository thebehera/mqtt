@file:Suppress("EXPERIMENTAL_API_USAGE")

package mqtt.socket

import mqtt.buffer.allocateNewBuffer
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
    suspend fun read(buffer: PlatformBuffer, timeout: Duration): Int
    suspend fun read(timeout: Duration = 1.seconds) = read(timeout) { buffer, bytesRead -> buffer.readUtf8(bytesRead) }
    suspend fun <T> read(
        timeout: Duration = 1.seconds,
        bufferRead: suspend (PlatformBuffer, Int) -> T
    ): SocketDataRead<T> {
        val buffer = allocateNewBuffer(8192u)
        buffer.resetForWrite()
        val bytesRead = read(buffer, timeout)
        val result = bufferRead(buffer, bytesRead)
        return SocketDataRead(result, bytesRead)
    }

    suspend fun write(buffer: PlatformBuffer, timeout: Duration = 1.seconds): Int
    suspend fun write(buffer: String, timeout: Duration = 1.seconds): Int
            = write(buffer.toBuffer().also { it.position(it.limit().toInt()) }, timeout)

}

data class SocketDataRead<T>(val result: T, val bytesRead: Int)

@ExperimentalTime
suspend fun openClientSocket(port: UShort,
                             timeout: Duration = 1.seconds,
                             hostname: String? = null,
                             socketOptions: SocketOptions? = null): ClientToServerSocket? {
    val socket = getClientSocket()
    if (socket == null) return null
    socket.open(port, timeout, hostname, socketOptions)
    return socket
}

@ExperimentalTime
fun getClientSocket(): ClientToServerSocket? {
    try {
        return asyncClientSocket()
    } catch (e: Throwable) {
        // failed to allocate async socket channel based socket, fallback to nio
    }
    return clientSocket(false)
}

@ExperimentalTime
expect fun asyncClientSocket(): ClientToServerSocket?

@ExperimentalTime
expect fun clientSocket(blocking: Boolean = false): ClientToServerSocket?

