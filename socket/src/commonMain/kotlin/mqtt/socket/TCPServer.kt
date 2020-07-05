package mqtt.socket

import kotlinx.coroutines.channels.ClosedReceiveChannelException
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

    suspend fun getClientConnection(socketCallbackException: ((Exception)-> Unit)? = null) {
        listen().collect {
            if (it != null) {
                try {
                    process.newInstance().startProcessing(it)
                } catch (c: ClosedReceiveChannelException) {
                    // do nothing as this expcetion is expected
                } catch (e: Exception) {
                    if (socketCallbackException != null)
                        socketCallbackException(e)
                } finally {
                    it.close()
                }
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
                if (client != null) {
                    emit(client)
                }
            }
        } catch (e: Exception) {
            throw e
        } finally {
            close()
        }
    }
}
