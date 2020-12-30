@file:Suppress("EXPERIMENTAL_API_USAGE")

package mqtt.socket

import mqtt.buffer.BufferPool
import platform.posix.init_sockets
import kotlin.time.ExperimentalTime

private var initialized = false
private fun initSockets() {
    if (!initialized) {
        init_sockets()
        initialized = true
    }
}

@ExperimentalTime
actual fun asyncClientSocket(pool: BufferPool) = clientSocket(false, pool)

@ExperimentalTime
actual fun clientSocket(blocking: Boolean, pool: BufferPool): ClientToServerSocket? {
    initSockets()
    return PosixClientToServerSocket(pool)
}

@ExperimentalTime
actual fun asyncServerSocket(): ServerSocket? {
    initSockets()
//    throw UnsupportedOperationException("Server not ready yet")
    return PosixServerSocket()
}

//actual suspend fun readStats(port: UShort, contains: String): List<String> = emptyList()


internal fun swapBytes(v: UShort): UShort =
    (((v.toInt() and 0xFF) shl 8) or ((v.toInt() ushr 8) and 0xFF)).toUShort()