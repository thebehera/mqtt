@file:Suppress("EXPERIMENTAL_API_USAGE")

package mqtt.socket

import mqtt.buffer.BufferPool
import kotlin.time.ExperimentalTime


@ExperimentalTime
actual fun asyncClientSocket(pool: BufferPool) = clientSocket(false, pool)

@ExperimentalTime
actual fun clientSocket(blocking: Boolean, pool: BufferPool): ClientToServerSocket {

    throw UnsupportedOperationException("not implemented yet")
}

@ExperimentalTime
actual fun asyncServerSocket(): ServerSocket {
    throw UnsupportedOperationException("not implemented yet")
}

actual suspend fun readStats(port: UShort, contains: String): List<String> = emptyList()