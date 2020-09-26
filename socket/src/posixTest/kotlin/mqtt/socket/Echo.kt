package mqtt.socket

import kotlinx.cinterop.*
import platform.posix.*
import kotlin.test.Test


class Echo {


    @Test
    fun main() {

        val port = 3000.toShort()

        // Initialize sockets in platform-dependent way.
        init_sockets()

        memScoped {

            val buffer = ByteArray(1024)
            val prefixBuffer = "echo: ".encodeToByteArray()
            val serverAddr = alloc<sockaddr_in>()

            val listenFd = socket(AF_INET, SOCK_STREAM, 0)
                .ensureUnixCallResult("socket") { !it.isMinusOne() }

            with(serverAddr) {
                memset(this.ptr, 0, sockaddr_in.size.convert())
                sin_family = AF_INET.convert()
                sin_port = posix_htons(port).convert()
            }

            bind(listenFd, serverAddr.ptr.reinterpret(), sockaddr_in.size.convert())
                .ensureUnixCallResult("bind") { it == 0 }

            listen(listenFd, 10)
                .ensureUnixCallResult("listen") { it == 0 }

            val commFd = accept(listenFd, null, null)
                .ensureUnixCallResult("accept") { !it.isMinusOne() }

            buffer.usePinned { pinned ->
                while (true) {
                    val length = recv(commFd, pinned.addressOf(0), buffer.size.convert(), 0).toInt()
                        .ensureUnixCallResult("read") { it >= 0 }

                    if (length == 0) {
                        break
                    }

                    send(commFd, prefixBuffer.refTo(0), prefixBuffer.size.convert(), 0)
                        .ensureUnixCallResult("write") { it >= 0 }
                    send(commFd, pinned.addressOf(0), length.convert(), 0)
                        .ensureUnixCallResult("write") { it >= 0 }
                }
            }
        }
    }
}


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