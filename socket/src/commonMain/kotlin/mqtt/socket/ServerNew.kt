package mqtt.socket

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.InternalCoroutinesApi
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.launch
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

       this.listenx().collect {
           process.process(client)
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


