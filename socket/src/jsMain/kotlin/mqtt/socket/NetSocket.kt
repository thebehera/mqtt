package mqtt.socket

import org.khronos.webgl.Uint8Array

@JsModule("net")
@JsNonModule
external class Net {
    companion object {
        fun connect(tcpOptions: tcpOptions, connectListener: () -> Unit): Socket
        fun createServer(connectionListener: (Socket) -> Unit = definedExternally): Server
    }
}

external class Server {
    fun on(event: String, callback: () -> Unit)
    fun on(event: String, callback: (Any) -> Unit)
    fun address(): IpAddress?
    fun close(callback: () -> Unit): Server
    fun getConnections(callback: (err: Any, count: Int) -> Unit): Server
    fun listen(
        port: Int = definedExternally,
        host: String = definedExternally,
        backlog: Int = definedExternally,
        callback: () -> Unit = definedExternally
    ): Server

    var listening: Boolean = definedExternally
    var maxConnections: Int = definedExternally
    fun ref(): Server
    fun unref(): Server
}


external class IpAddress {
    val port: Int
    val family: String
    val address: String
}

external class Socket {
    var localPort: Int
    var remotePort: Int
    var remoteAddress: String?
    fun write(data: Uint8Array, callback: () -> Unit): Boolean
    fun on(event: String, callback: () -> Unit)
    fun on(event: String, callback: (Any) -> Unit)
    fun pipe(socket: Socket): Socket
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
