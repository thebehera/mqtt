package mqtt.socket

import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.flow
import kotlin.time.ExperimentalTime

@ExperimentalUnsignedTypes
@ExperimentalTime
class ServerNew <T : ServerSocket> (val host: String, val port: UShort, val process: ServerProcess) {
    private lateinit var serverSocket : T

    suspend fun start() {
        serverSocket = asyncServerSocket() as T
        serverSocket.bind(port, host)
    }

    suspend fun getClient() {
        var client : ClientSocket

        listenx().collect {
            process.process(it)
        }

    }

    suspend fun listenx () = flow {
        try {
            while (serverSocket.isOpen()) {
                val client = serverSocket.accept()

                emit(client)
            }
        } catch (e: Exception) {

        }
    }
}


