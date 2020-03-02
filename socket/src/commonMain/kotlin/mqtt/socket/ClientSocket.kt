package mqtt.socket

import mqtt.buffer.PlatformBuffer
import mqtt.buffer.SuspendCloseable
import kotlin.time.Duration
import kotlin.time.ExperimentalTime

@ExperimentalTime
@ExperimentalUnsignedTypes
interface ClientSocket : SuspendCloseable {
    fun isOpen(): Boolean
    fun localPort(): UShort?
    fun remotePort(): UShort?
    suspend fun read(buffer: PlatformBuffer, timeout: Duration): Int
    suspend fun write(buffer: PlatformBuffer, timeout: Duration): Int
}

@ExperimentalUnsignedTypes
@ExperimentalTime
expect fun asyncClientSocket(): ClientToServerSocket

@ExperimentalUnsignedTypes
@ExperimentalTime
expect fun clientSocket(blocking: Boolean): ClientToServerSocket

