package mqtt.socket

import mqtt.buffer.BufferPool
import kotlin.browser.window
import kotlin.time.ExperimentalTime

val isNodeJs by lazy {
    try {
        window
        false
    } catch (t: Throwable) {
        true
    }
}

@ExperimentalTime
actual fun asyncClientSocket(pool: BufferPool): ClientToServerSocket {
    return if (isNodeJs) {
        NodeClientSocket()
    } else {
        throw UnsupportedOperationException("Sockets are not supported in the browser")
    }
}


@ExperimentalTime
actual fun clientSocket(blocking: Boolean, pool: BufferPool): ClientToServerSocket =
    throw UnsupportedOperationException("Only non blocking io is supported with JS")

@ExperimentalUnsignedTypes
@ExperimentalTime
actual fun asyncServerSocket(): ServerSocket {
    if (isNodeJs) {
        return NodeServerSocket()
    } else {
        throw UnsupportedOperationException("Sockets are not supported in the browser")
    }
}

external fun require(module: String): dynamic