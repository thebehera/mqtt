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
            val x = process.newInstance()
            x.startProcessing(it)
        }
    }

    suspend fun getListenPort() : UShort {
        val x = if (serverSocket.port() != null) serverSocket.port() as UShort else 0u
        return x
    }

    suspend fun close() {
        if (serverSocket.isOpen())
            serverSocket.close()
    }

    private suspend fun listen () = flow {
        try {
            while (serverSocket.isOpen()) {
                val client = serverSocket.accept()

                emit(client)
            }
        } catch (e: Exception) {
            if (! e.toString().equals("java.nio.channels.AsynchronousCloseException"))
                println("listen exception: $e, ${e.message}")
        }
        close()
    }
}
