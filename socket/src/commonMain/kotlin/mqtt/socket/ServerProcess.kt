package mqtt.socket

import kotlinx.coroutines.InternalCoroutinesApi
import kotlin.time.ExperimentalTime

interface ServerProcess {
    @ExperimentalTime
    suspend fun process (socket : ClientSocket)
    suspend fun close()
}
