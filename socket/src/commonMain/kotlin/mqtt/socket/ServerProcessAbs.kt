package mqtt.socket

import kotlinx.coroutines.InternalCoroutinesApi
import kotlin.time.ExperimentalTime

abstract class ServerProcessAbs {
    @ExperimentalTime
    protected lateinit var socket: ClientSocket

    abstract suspend fun serverSideProcess()

    @ExperimentalTime
    suspend fun startProcessing (socket : ClientSocket) {
        try {
            this.socket = socket
            serverSideProcess()
        } catch (e: Exception) {

        } finally {
            close()
        }
    }

    @ExperimentalTime
    suspend fun close() {
        try {
            if (socket?.isOpen())
                socket?.close()
        } catch (e: Exception) {}
    }
}
