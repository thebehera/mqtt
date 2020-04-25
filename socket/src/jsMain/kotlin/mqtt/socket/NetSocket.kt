package mqtt.socket

import org.khronos.webgl.Uint8Array

@JsModule("net")
@JsNonModule
external class Net {
    fun connect(tcpOptions: tcpOptions, connectListener: (Socket) -> Unit): Socket
}

external class Socket {
    var localPort: Int
    var remotePort: Int
    var remoteAddress: String?
    fun write(data: Uint8Array, callback: () -> Unit): Boolean
    fun on(event: String, callback: () -> Unit)
    fun on(event: String, callback: (Any) -> Unit)
    fun pause(): Socket
    fun resume(): Socket
    fun end(callback: () -> Unit): Socket
}

class OnRead(val buffer: () -> Uint8Array, val callback: (Int, Uint8Array) -> Boolean)

class tcpOptions(
    val port: Int,
    val host: String? = null,
    val onread: OnRead
)
