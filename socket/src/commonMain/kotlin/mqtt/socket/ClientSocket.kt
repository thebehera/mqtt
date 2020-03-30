@file:Suppress("EXPERIMENTAL_API_USAGE")

package mqtt.socket

import mqtt.buffer.PlatformBuffer
import mqtt.buffer.SuspendCloseable
import kotlin.time.Duration
import kotlin.time.ExperimentalTime

@ExperimentalTime
interface ClientSocket : SuspendCloseable {
    fun isOpen(): Boolean
    fun localPort(): UShort?
    fun remotePort(): UShort?
    suspend fun read(buffer: PlatformBuffer, timeout: Duration): Int
    suspend fun write(buffer: PlatformBuffer, timeout: Duration): Int
}

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
expect fun asyncClientSocket(): ClientToServerSocket

@ExperimentalTime
expect fun clientSocket(blocking: Boolean): ClientToServerSocket

