package mqtt.socket

import mqtt.buffer.PlatformBuffer
import kotlin.time.Duration
import kotlin.time.ExperimentalTime
import kotlin.time.seconds

enum class ConnectionType {ASYNC, NON_BLOCKING, BLOCKING}

abstract class TCPClientProcess (val connectionType: ConnectionType = ConnectionType.ASYNC) : ClientProcess {
    @ExperimentalTime
    private lateinit var socket: ClientSocket

    @ExperimentalTime
    override suspend fun connect(host: String, port: UShort) {
        try {
            val client = when (connectionType) {
                ConnectionType.ASYNC -> asyncClientSocket()
                ConnectionType.BLOCKING -> clientSocket(true)
                ConnectionType.NON_BLOCKING -> clientSocket(false)
            }

            client.open(port, 100.seconds, host)
            if (client.isOpen()) {
                socket = client
                clientSideProcess()
            }
        } catch (e: Exception) {
            throw e
        } finally {
            close()
        }
    }

    @ExperimentalTime
    override suspend fun isOpen(): Boolean {
        return socket.isOpen()
    }

    @ExperimentalTime
    override suspend fun remotePort(): UShort? {
        return socket.remotePort()
    }

    @ExperimentalTime
    override suspend fun <T> read(timeout: Duration, buffer: (PlatformBuffer, Int) -> T ) =
        socket.read(timeout, buffer)

    @ExperimentalTime
    override suspend fun write(buffer: PlatformBuffer, timeout: Duration): Int {
        return socket.write(buffer, timeout)
    }

    @ExperimentalTime
    override suspend fun close() {
        try {
            if (socket.isOpen())
                socket.close()
        } catch (e: Exception) {}
    }
}