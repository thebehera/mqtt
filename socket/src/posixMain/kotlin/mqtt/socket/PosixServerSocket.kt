package mqtt.socket

import kotlinx.cinterop.*
import platform.posix.*

class PosixServerSocket : ServerSocket {
    private val fileDescriptor: Int
    private val memScope = MemScope()

    init {
        fileDescriptor = socket(AF_INET, SOCK_STREAM, 0)
            .ensureUnixCallResult("socket") { !it.isMinusOne() }
    }

    override suspend fun bind(
        port: UShort?,
        host: String?,
        socketOptions: SocketOptions?,
        backlog: UInt
    ): SocketOptions {
        val actualPort = port?.toShort() ?: 0
        with(memScope) {
            val serverAddr = alloc<sockaddr_in>()
            with(serverAddr) {
                memset(this.ptr, 0, sockaddr_in.size.convert())
                sin_family = AF_INET.convert()
                sin_port = posix_htons(actualPort).convert()
            }
            bind(fileDescriptor, serverAddr.ptr.reinterpret(), sockaddr_in.size.convert())
                .ensureUnixCallResult("bind") { it == 0 }
        }
        listen(fileDescriptor, backlog.toInt())
            .ensureUnixCallResult("listen") { it == 0 }
        return SocketOptions()
    }

    override suspend fun accept(): ClientSocket {
        val acceptedClientFileDescriptor = accept(fileDescriptor, null, null)
            .ensureUnixCallResult("accept") { !it.isMinusOne() }
        val server2Client = PosixClientSocket()
        server2Client.currentFileDescriptor = acceptedClientFileDescriptor
        return server2Client

    }

    override fun isOpen() = try {
        port()
        true
    } catch (e: Throwable) {
        false
    }


    override fun port(): UShort? = memScoped {
        val localAddress = alloc<sockaddr_in>()
        val addressLength = alloc<socklen_tVar>()
        addressLength.value = sockaddr_in.size.convert()
        if (getsockname(fileDescriptor, localAddress.ptr.reinterpret(), addressLength.ptr) < 0) null
        else swapBytes(localAddress.sin_port)
    }

    override suspend fun close() {
        close(fileDescriptor)
    }

    private inline fun Int.ensureUnixCallResult(op: String, predicate: (Int) -> Boolean): Int {
        if (!predicate(this)) {
            throw Error("$op: ${strerror(posix_errno())!!.toKString()}")
        }
        return this
    }

    private fun Int.isMinusOne() = (this == -1)
}