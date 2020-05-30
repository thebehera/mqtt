package mqtt.socket

import kotlinx.coroutines.InternalCoroutinesApi
import kotlin.time.ExperimentalTime

abstract class TCPServerProcess : ServerProcess {
    @ExperimentalTime
  protected lateinit var socket: ClientSocket

    @ExperimentalTime
    abstract override suspend fun serverSideProcess()

    abstract override suspend fun newInstance(): ServerProcess

    @ExperimentalTime
    override suspend fun startProcessing(socket: ClientSocket) {
        try {
            this.socket = socket
            serverSideProcess()
        } catch (e: Exception) {
            throw e
        } finally {
            close()
        }
    }

    @ExperimentalTime
    override suspend fun close() {
        try {
            if (socket.isOpen())
                socket.close()
        } catch (e: Exception) {}
    }
}
