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
open class PosixClientSocket(override val pool: BufferPool = BufferPool()) : ClientSocket {

    var currentFileDescriptor: Int? = null

    override fun isOpen() = localPort() != null && remotePort() != null

    override fun localPort() = memScoped {
        val currentFileDescriptor = currentFileDescriptor ?: return null
        val localAddress = alloc<sockaddr_in>()
        val addressLength = alloc<socklen_tVar>()
        addressLength.value = sockaddr_in.size.convert()
        if (getsockname(currentFileDescriptor, localAddress.ptr.reinterpret(), addressLength.ptr) < 0) null
        else swapBytes(localAddress.sin_port)
    }

    override fun remotePort(): UShort? = memScoped {
        val currentFileDescriptor = currentFileDescriptor ?: return null
        val peerAddress = alloc<sockaddr_in>()
        val addressLength = alloc<socklen_tVar>()
        addressLength.value = sockaddr_in.size.convert()
        if (getpeername(currentFileDescriptor, peerAddress.ptr.reinterpret(), addressLength.ptr) < 0) null
        else swapBytes(peerAddress.sin_port)
    }

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

    override suspend fun read(buffer: PlatformBuffer, timeout: Duration): Int {
        val nativeBuffer = buffer as NativeBuffer
        return buffer.data.usePinned { pinned ->
            recv(currentFileDescriptor!!, pinned.addressOf(0), buffer.capacity.toInt().convert(), 0)
                    .ensureUnixCallResult("read") { it >= 0 }.toInt()
        }
    }

    override suspend fun write(buffer: PlatformBuffer, timeout: Duration): Int {
        val currentFileDescriptor = currentFileDescriptor ?: return 0
        buffer.resetForRead()
        return send(
            currentFileDescriptor,
            (buffer as NativeBuffer).data.refTo(0),
            (buffer.limit() - buffer.position()).toInt().convert(), 0
        ).ensureUnixCallResult("write") { it >= 0 }.toInt()
    }

    override suspend fun close() {
        currentFileDescriptor?.let { close(it) }
    }

    protected inline fun Int.ensureUnixCallResult(op: String, predicate: (Int) -> Boolean): Int {
        if (!predicate(this)) {
            currentFileDescriptor = null
            throw Error("$op: ${strerror(posix_errno())!!.toKString()}")
        }
        return this
    }

    private inline fun Long.ensureUnixCallResult(op: String, predicate: (Long) -> Boolean): Long {
        if (!predicate(this)) {
            currentFileDescriptor = null
            throw Error("$op: ${strerror(posix_errno())!!.toKString()}")
        }
        return this
    }

    protected fun Int.isMinusOne() = (this == -1)
}