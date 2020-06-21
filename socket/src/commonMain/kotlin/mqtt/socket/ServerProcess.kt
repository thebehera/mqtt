package mqtt.socket

import kotlinx.coroutines.InternalCoroutinesApi
import mqtt.buffer.PlatformBuffer
import kotlin.time.Duration
import kotlin.time.ExperimentalTime

interface ServerProcess {
    @ExperimentalTime
    suspend fun serverSideProcess ()
    suspend fun newInstance() : ServerProcess
    @ExperimentalTime
    suspend fun startProcessing (socket: ClientSocket)
    suspend fun isOpen(): Boolean
    @ExperimentalTime
    suspend fun read(buffer: PlatformBuffer, timeout: Duration): Int
    @ExperimentalTime
    suspend fun write(buffer: PlatformBuffer, timeout: Duration): Int
    suspend fun close()
}
