package mqtt.socket

import kotlinx.cinterop.*
import mqtt.buffer.BufferPool
import platform.posix.*
import kotlin.time.Duration

class PosixClientToServerSocket(pool: BufferPool) : PosixClientSocket(pool), ClientToServerSocket {
    private val memScope = MemScope()

    override suspend fun open(
        port: UShort,
        timeout: Duration,
        hostname: String?,
        socketOptions: SocketOptions?
    ): SocketOptions {
        val correctedHostName = hostname ?: "localhost"
        val host = gethostbyname(correctedHostName) ?: throw Exception("Unknown host: $correctedHostName")
        val socketFileDescriptor = socket(AF_INET, SOCK_STREAM, 0)
            .ensureUnixCallResult("socket") { !it.isMinusOne() }
        with(memScope) {
            val serverAddr = alloc<sockaddr_in>()
            with(serverAddr) {
                memset(this.ptr, 0, sockaddr_in.size.convert())
                sin_family = AF_INET.convert()
                sin_port = posix_htons(port.toShort()).convert()
                sin_addr.s_addr = host.pointed.h_addr_list!![0]!!.reinterpret<UIntVar>().pointed.value
            }
            connect(socketFileDescriptor, serverAddr.ptr.reinterpret(), sockaddr_in.size.convert())
                .ensureUnixCallResult("connect") { !it.isMinusOne() }
            currentFileDescriptor = socketFileDescriptor
        }
        return SocketOptions()
    }

}