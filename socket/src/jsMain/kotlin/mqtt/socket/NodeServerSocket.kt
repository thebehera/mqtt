package mqtt.socket

import kotlinx.coroutines.channels.Channel
import mqtt.buffer.JsBuffer
import org.khronos.webgl.Uint8Array
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

class NodeServerSocket : ServerSocket {
    var server: Server? = null
    private val clientSocketChannel = Channel<ClientSocket>(Channel.UNLIMITED)

    override suspend fun bind(
        port: UShort?,
        host: String?,
        socketOptions: SocketOptions?,
        backlog: UInt
    ): SocketOptions {
        val server = Net.createServer { clientSocket ->
            val nodeSocket = NodeSocket()
            nodeSocket.netSocket = clientSocket
            clientSocket.on("data") { data ->
                val result = uint8ArrayOf(data)
                val buffer = JsBuffer(result)
                buffer.setPosition(0)
                buffer.setLimit(result.length)
                nodeSocket.incomingMessageChannel.offer(SocketDataRead(buffer, result.length))
            }
            clientSocketChannel.offer(nodeSocket)
        }
        server.listenSuspend(port, host, backlog)
        this.server = server
        return socketOptions ?: SocketOptions()
    }

    @Suppress("MemberVisibilityCanBePrivate")
    fun uint8ArrayOf(@Suppress("UNUSED_PARAMETER") obj: Any): Uint8Array {
        return js(
            """
            if (Buffer.isBuffer(obj)) {
                return new Uint8Array(obj.buffer)
            } else {
                return new Uint8Array(Buffer.from(obj).buffer)
            }
        """
        ) as Uint8Array
    }

    override suspend fun accept(): ClientSocket {
        val clientSocket = clientSocketChannel.receive()
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