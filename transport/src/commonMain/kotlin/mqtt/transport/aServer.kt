package mqtt.transport

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import mqtt.connection.ServerControlPacketTransport
import mqtt.transport.nio.socket.readStats
import kotlin.time.ExperimentalTime

@ExperimentalTime
expect fun aServer(
    scope: CoroutineScope,
    maxBufferSize: Int,
    group: Any? = null
): ServerControlPacketTransport


@ExperimentalTime
interface Server {
    val connections: Map<UShort, ClientSocket>
    suspend fun listen(): Flow<ClientSocket>
    suspend fun closeClient(port: UShort)
    fun getStats(): List<String>
}


@ExperimentalTime
@ExperimentalUnsignedTypes
class SocketServer(val serverSocket: ServerSocket) : Server {
    override val connections = HashMap<UShort, ClientSocket>()
    override suspend fun listen() = flow {
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

    override suspend fun closeClient(port: UShort) {
        connections.remove(port)?.close()
    }

    suspend fun close() {
        if (!serverSocket.isOpen() && connections.isNotEmpty()) {
            return
        }
        connections.values.forEach { it.close() }
        connections.clear()
    }

    override fun getStats() = readStats(serverSocket.port()!!, "CLOSE_WAIT")
}

