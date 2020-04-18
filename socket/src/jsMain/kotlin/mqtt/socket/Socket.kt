package mqtt.socket

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
actual fun asyncClientSocket(): ClientToServerSocket {
    return if (isNodeJs) {
//        NodeClientSocket()
        throw UnsupportedOperationException("Implementation WIP")
    } else {
        throw UnsupportedOperationException("Sockets are not supported in the browser")
    }
}


@ExperimentalTime
actual fun clientSocket(blocking: Boolean) = asyncClientSocket()


@ExperimentalUnsignedTypes
@ExperimentalTime
actual fun asyncServerSocket(): ServerSocket {
    if (isNodeJs) {
        throw UnsupportedOperationException("Implementation WIP")
    } else {
        throw UnsupportedOperationException("Sockets are not supported in the browser")
    }
}

external fun require(module: String): dynamic