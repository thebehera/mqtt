package mqtt.socket

import kotlin.browser.window
import kotlin.time.ExperimentalTime

fun isNodeJs(): Boolean {
    return try {
        !(js("'WebSocket' in window || 'MozWebSocket' in window") as Boolean)
    } catch (t: Throwable) {
        true
    }
}

@ExperimentalTime
actual fun asyncClientSocket(): ClientToServerSocket? {
    return if (isNodeJs()) {
        val net = require("net")
        NodeClientSocket()
    } else {
        return null
    }
}


@ExperimentalTime
actual fun clientSocket(blocking: Boolean): ClientToServerSocket? = null

@ExperimentalUnsignedTypes
@ExperimentalTime
actual fun asyncServerSocket(): ServerSocket? {
    if (isNodeJs()) {
        val net = require("net")
//        throw UnsupportedOperationException("Not implemented yet")
        return NodeServerSocket()
    } else {
        return null
    }
}

external fun require(module: String): dynamic