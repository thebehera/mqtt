package mqtt.socket

import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.flow
import kotlin.time.ExperimentalTime

@ExperimentalUnsignedTypes
@ExperimentalTime
class TCPServer (val host: String, val port: UShort, val process: ServerProcess) {
    private lateinit var serverSocket : ServerSocket

    suspend fun startServer() {
        val tPort :UShort? = if (port > 0u)  port else null
        serverSocket = asyncServerSocket()
        if (!serverSocket.isOpen())
            serverSocket.bind(tPort, host)
    }

    suspend fun isOpen() : Boolean = serverSocket.isOpen()

    suspend fun getClientConnection() {
        listen().collect {
            if (it != null) {
                process.newInstance().startProcessing(it)
            }
        }
    }

    suspend fun getListenPort() : UShort = if (serverSocket.port() != null) serverSocket.port() as UShort else 0u

    suspend fun close() {
        if (isOpen())
            serverSocket.close()
    }

    private suspend fun listen () = flow {
        try {
            while (serverSocket.isOpen()) {
                val client = serverSocket.accept()
                if (client != null)
                    emit(client)
            }
        } catch (e: Exception) {
            throw e
        } finally {
            close()
        }
    }
}
