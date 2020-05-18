package mqtt.socket

import kotlinx.coroutines.InternalCoroutinesApi
import kotlin.time.ExperimentalTime

interface ServerProcess {
    @ExperimentalTime
    suspend fun serverSideProcess ()
    @ExperimentalTime
    suspend fun startProcessing (socket : ClientSocket)
    suspend fun close()
}
