package mqtt.socket

import kotlinx.coroutines.flow.flow
import kotlin.time.ExperimentalTime

@ExperimentalTime
@ExperimentalUnsignedTypes
class Server(val serverSocket: ServerSocket) {
    val connections = HashMap<UShort, ClientSocket>()
    suspend fun listen() = flow {
        try {
            while (serverSocket.isOpen()) {
                val client = serverSocket.accept()
                connections[client.remotePort()!!] = client
                emit(client)
            }
        } catch (e: Throwable) {
            // we're done
        }
        serverSocket.close()
    }

    suspend fun closeClient(port: UShort) {
        connections.remove(port)?.close()
    }

    suspend fun close() {
        if (!serverSocket.isOpen() && connections.isNotEmpty()) {
            return
        }
        connections.values.forEach { it.close() }
        connections.clear()
    }

    fun getStats() = readStats(serverSocket.port()!!, "CLOSE_WAIT")
}

