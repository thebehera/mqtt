package mqtt.socket

class NodeServerSocket : ServerSocket {
    override suspend fun bind(
        port: UShort?,
        host: String?,
        socketOptions: SocketOptions?,
        backlog: UInt
    ): SocketOptions {

        return socketOptions!!
    }

    override suspend fun accept(): ClientSocket {
        TODO()
    }

    override fun isOpen(): Boolean {
        return false
    }

    override fun port(): UShort? {
        return null
    }

    override suspend fun close() {
    }
}