package mqtt.socket

import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.flow
import kotlin.time.ExperimentalTime

@ExperimentalUnsignedTypes
@ExperimentalTime
class TCPServer (val host: String, val port: UShort, val process: ServerProcess) {
    private lateinit var serverSocket : ServerSocket

    suspend fun startServer() {
        val x = 0u
        val nPort :UShort? = if (port > x)  port else null
        serverSocket = asyncServerSocket()
        if (!serverSocket.isOpen())
            serverSocket.bind(nPort, host)
    }

    suspend fun isOpen() : Boolean {
        return serverSocket.isOpen()
    }
    suspend fun getClientConnection() {

        listen().collect {
            if (it != null) {
                process.newInstance().startProcessing(it)
            }
        }
    }

    suspend fun getListenPort() : UShort = if (serverSocket.port() != null) serverSocket.port() as UShort else 0u

    suspend fun close() {
        if (serverSocket.isOpen())
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
            println("listen exception: $e, ${e.message}")
            throw e
        } finally {
            close()
        }
    }
}
