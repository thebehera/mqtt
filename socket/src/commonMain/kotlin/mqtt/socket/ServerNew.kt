package mqtt.socket

import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.flow
import kotlin.time.ExperimentalTime

@ExperimentalUnsignedTypes
@ExperimentalTime
class ServerNew (val host: String, val port: UShort, val process: ServerProcessAbs) {
    private lateinit var serverSocket : ServerSocket

    suspend fun startServer() {
        serverSocket = asyncServerSocket()
        if (!serverSocket.isOpen())
            serverSocket.bind(port, host)
    }

    suspend fun isOpen() : Boolean {
        return serverSocket?.isOpen()
    }
    suspend fun getClientConnection() {

        listen().collect {
            process.startProcessing(it)
        }

    }

    suspend fun close() {
        if (serverSocket?.isOpen())
            serverSocket?.close()
    }

    private suspend fun listen () = flow {
        try {
            while (serverSocket?.isOpen()) {
                val client = serverSocket?.accept()

                emit(client)
            }
        } catch (e: Exception) {

        }
        close()
    }
}
