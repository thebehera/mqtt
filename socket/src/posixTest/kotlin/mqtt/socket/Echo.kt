package mqtt.socket

import kotlinx.cinterop.toKString
import platform.posix.posix_errno
import platform.posix.strerror


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