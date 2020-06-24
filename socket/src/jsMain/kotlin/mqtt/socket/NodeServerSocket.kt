package mqtt.socket

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.launch
import kotlin.coroutines.coroutineContext
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

class NodeServerSocket : ServerSocket {
    var server: Server? = null
    private val clientSocketChannel = Channel<Socket>()

    override suspend fun bind(
        port: UShort?,
        host: String?,
        socketOptions: SocketOptions?,
        backlog: UInt
    ): SocketOptions {
        val ctx = CoroutineScope(coroutineContext)
        val server = Net.createServer { clientSocket ->
            clientSocket.pause()
            ctx.launch {
                clientSocketChannel.send(clientSocket)
            }
        }
        server.listenSuspend(port, host, backlog)
        this.server = server
        return socketOptions ?: SocketOptions()
    }

    override suspend fun accept(): ClientSocket {
        val clientSocket = NodeSocket()
        clientSocket.netSocket = clientSocketChannel.receive()
        clientSocket.netSocket?.resume()
        return clientSocket
    }

    override fun isOpen() = server?.listening ?: false

    override fun port() = server?.address()?.port?.toUShort()

    override suspend fun close() {
        val server = server ?: return
        suspendCoroutine<Unit> {
            server.close { it.resume(Unit) }
        }
    }
}

suspend fun Server.listenSuspend(port: UShort?, host: String?, backlog: UInt) {
    suspendCoroutine<Unit> {
        if (host != null && port != null) {
            listen(port.toInt(), host, backlog.toInt()) {
                it.resume(Unit)
            }
        } else if (port != null) {
            listen(port.toInt(), backlog = backlog.toInt()) {
                it.resume(Unit)
            }
        } else if (host != null) {
            listen(host = host, backlog = backlog.toInt()) {
                it.resume(Unit)
            }
        } else {
            listen(backlog = backlog.toInt()) {
                it.resume(Unit)
            }
        }
    }
}