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
actual fun asyncClientSocket(pool: BufferPool): ClientToServerSocket? {

    return if (isNodeJs) {
        val net = require("net")
        println(net)
        NodeClientSocket()
    } else {
        return null
    }
}


@ExperimentalTime
actual fun clientSocket(blocking: Boolean, pool: BufferPool): ClientToServerSocket? = null

@ExperimentalUnsignedTypes
@ExperimentalTime
actual fun asyncServerSocket(): ServerSocket? {
    if (isNodeJs) {
        val net = require("net")
//        throw UnsupportedOperationException("Not implemented yet")
        return NodeServerSocket()
    } else {
        return null
    }
}

external fun require(module: String): dynamic