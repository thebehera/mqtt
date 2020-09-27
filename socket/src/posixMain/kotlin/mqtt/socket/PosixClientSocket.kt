package mqtt.socket

import kotlinx.cinterop.*
import kotlinx.coroutines.withTimeout
import mqtt.buffer.BufferPool
import mqtt.buffer.NativeBuffer
import mqtt.buffer.PlatformBuffer
import platform.posix.*
import kotlin.time.Duration
import kotlin.time.ExperimentalTime

@ExperimentalUnsignedTypes
@ExperimentalTime
class PosixClientSocket(private val pool: BufferPool = BufferPool()) : ClientToServerSocket {
    val memScope = MemScope()
    var currentFileDescriptor: Int? = null


    override suspend fun open(
        port: UShort,
        timeout: Duration,
        hostname: String?,
        socketOptions: SocketOptions?
    ): SocketOptions {
        init_sockets()
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

    override fun isOpen() = currentFileDescriptor != null

    override fun localPort() = 0.toUShort()

    override fun remotePort() = 0.toUShort()

    override suspend fun <T> read(timeout: Duration, bufferRead: (PlatformBuffer, Int) -> T): SocketDataRead<T> {
        return withTimeout(timeout) {
            pool.borrowSuspend { buffer ->
                val nativeBuffer = buffer as NativeBuffer
                buffer.data.usePinned { pinned ->
                    val bytesRead =
                        recv(currentFileDescriptor!!, pinned.addressOf(0), buffer.capacity.toInt().convert(), 0)
                            .ensureUnixCallResult("read") { it >= 0 }
                    SocketDataRead(bufferRead(nativeBuffer, bytesRead.toInt()), bytesRead.toInt())
                }

            }
        }
    }

    override suspend fun write(buffer: PlatformBuffer, timeout: Duration): Int {
        val currentFileDescriptor = currentFileDescriptor ?: return 0
        buffer.resetForRead()
        return send(
            currentFileDescriptor,
            (buffer as NativeBuffer).data.refTo(0),
            (buffer.limit() - buffer.position()).toInt().convert(), 0
        )
            .ensureUnixCallResult("write") { it >= 0 }.toInt()
    }

    override suspend fun close() {
        val currentFileDescriptor = currentFileDescriptor
        if (currentFileDescriptor != null) {
            close(currentFileDescriptor)
            this.currentFileDescriptor = null
        }
    }

    private inline fun Int.ensureUnixCallResult(op: String, predicate: (Int) -> Boolean): Int {
        if (!predicate(this)) {
            throw Error("$op: ${strerror(posix_errno())!!.toKString()}")
        }
        return this
    }

    private fun Int.isMinusOne() = (this == -1)

    companion object {
        inline fun Int.ensureUnixCallResult(op: String, predicate: (Int) -> Boolean): Int {
            if (!predicate(this)) {
                throw Error("$op: ${strerror(posix_errno())!!.toKString()}")
            }
            return this
        }

        inline fun Long.ensureUnixCallResult(op: String, predicate: (Long) -> Boolean): Long {
            if (!predicate(this)) {
                throw Error("$op: ${strerror(posix_errno())!!.toKString()}")
            }
            return this
        }

        inline fun ULong.ensureUnixCallResult(op: String, predicate: (ULong) -> Boolean): ULong {
            if (!predicate(this)) {
                throw Error("$op: ${strerror(posix_errno())!!.toKString()}")
            }
            return this
        }

        private fun Int.isMinusOne() = (this == -1)
        private fun Long.isMinusOne() = (this == -1L)
        private fun ULong.isMinusOne() = (this == ULong.MAX_VALUE)
    }

}