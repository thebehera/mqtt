package mqtt.socket

import mqtt.buffer.PlatformBuffer
import kotlin.time.Duration
import kotlin.time.ExperimentalTime

interface ClientProcess {
    suspend fun clientSideProcess()
    suspend fun connect(host: String, port:UShort)
    suspend fun remotePort(): UShort?
    @ExperimentalTime
    suspend fun <T> read(timeout: Duration, buffer: (PlatformBuffer, Int) -> T): SocketDataRead<T>
    @ExperimentalTime
    suspend fun write(buffer: PlatformBuffer, timeout: Duration): Int
    suspend fun isOpen(): Boolean
    suspend fun close()
}