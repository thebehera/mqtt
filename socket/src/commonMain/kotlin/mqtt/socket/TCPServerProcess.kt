package mqtt.socket

import kotlinx.coroutines.InternalCoroutinesApi
import mqtt.buffer.PlatformBuffer
import kotlin.time.Duration
import kotlin.time.ExperimentalTime

abstract class TCPServerProcess : ServerProcess {
    @ExperimentalTime
  private lateinit var socket: ClientSocket

    @ExperimentalTime
    abstract override suspend fun serverSideProcess()

    abstract override suspend fun newInstance(): ServerProcess

    @ExperimentalTime
    override suspend fun startProcessing(socket: ClientSocket) {
        this.socket = socket
        serverSideProcess()
    }

    @ExperimentalTime
    override suspend fun <T> read(timeout: Duration, bufferRead: (PlatformBuffer, Int) -> T)
        = socket.read(timeout, bufferRead)

    @ExperimentalTime
    override suspend fun write(buffer: PlatformBuffer, timeout: Duration): Int {
        return socket.write(buffer, timeout)
    }

    @ExperimentalTime
    override suspend fun isOpen(): Boolean {
        return socket.isOpen()
    }
    @ExperimentalTime
    override suspend fun close() {
        try {
            if (socket.isOpen())
                socket.close()
        } catch (e: Exception) {}
    }
}
