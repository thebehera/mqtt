package mqtt.socket

import kotlinx.cinterop.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import mqtt.buffer.BufferPool
import mqtt.buffer.NativeBuffer
import mqtt.buffer.PlatformBuffer
import platform.posix.*
import kotlin.time.Duration
import kotlin.time.ExperimentalTime

@ExperimentalUnsignedTypes
@ExperimentalTime
class PosixClientSocket(val pool: BufferPool = BufferPool()) : ClientToServerSocket {
    val memScope = MemScope()
    var currentFileDescriptor: Int? = null
    override suspend fun open(
        port: UShort,
        timeout: Duration,
        hostname: String?,
        socketOptions: SocketOptions?
    ): SocketOptions {
        val correctedHostName = hostname ?: "localhost"
        currentFileDescriptor = withContext(currentCoroutineContext() + Dispatchers.Unconfined) {
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
            }
            socketFileDescriptor
        }
        return SocketOptions()
    }

    override fun isOpen() = currentFileDescriptor != null

    override fun localPort() = 0.toUShort()

    override fun remotePort() = 0.toUShort()

    override suspend fun <T> read(timeout: Duration, bufferRead: (PlatformBuffer, Int) -> T): SocketDataRead<T> {
        return withTimeout(timeout) {
            pool.borrowSuspend { buffer ->
                val b = buffer as NativeBuffer
                SocketDataRead(bufferRead(b, 10), 10)
            }
        }
    }

    override suspend fun write(buffer: PlatformBuffer, timeout: Duration): Int {
        return 0
    }

    override suspend fun close() {
        val currentFileDescriptor = currentFileDescriptor
        if (currentFileDescriptor != null) {
            close(currentFileDescriptor)
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
        init {
            init_sockets()
        }
    }

}