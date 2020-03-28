package mqtt.socket

<<<<<<< HEAD
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.flow
=======
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.InternalCoroutinesApi
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.launch
>>>>>>> initial checkin
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

<<<<<<< HEAD
        listenx().collect {
            process.process(it)
        }
=======
       this.listenx().collect {
           process.process(client)
       }
>>>>>>> initial checkin

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


