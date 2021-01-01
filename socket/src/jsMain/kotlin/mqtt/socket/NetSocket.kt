package mqtt.socket

import org.khronos.webgl.Uint8Array

//@JsModule("net")
//@JsNonModule
external class net {
    companion object {
        fun connect(tcpOptions: tcpOptions, connectListener: () -> Unit): Socket
        fun connect(tcpOptions: TcpSocketConnectOpts, connectListener: () -> Unit = definedExternally): Socket
        fun createServer(connectionListener: (Socket) -> Unit = definedExternally): Server
    }
}
fun getNet():net = js("require('net')") as net
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
    fun destroy(error: dynamic = definedExternally): Socket
}

external interface OnReadOpts {
    var buffer: dynamic /* Uint8Array | () -> Uint8Array */
        get() = definedExternally
        set(value) = definedExternally

    fun callback(bytesWritten: Number, buf: Uint8Array): Boolean
}

external interface ConnectOpts {
    var onread: OnReadOpts?
        get() = definedExternally
        set(value) = definedExternally
}

open external interface TcpSocketConnectOpts : ConnectOpts {
    var port: Number?
        get() = definedExternally
        set(value) = definedExternally
    var host: String?
        get() = definedExternally
        set(value) = definedExternally
    var localAddress: String?
        get() = definedExternally
        set(value) = definedExternally
    var localPort: Number?
        get() = definedExternally
        set(value) = definedExternally
    var hints: Number?
        get() = definedExternally
        set(value) = definedExternally
    var family: Number?
        get() = definedExternally
        set(value) = definedExternally
}

class OnRead(
    var buffer: (() -> Uint8Array)? = null,
    var callback: ((Int, Any) -> Boolean)? = null
)

class tcpOptions(
    val port: Int,
    val host: String? = null,
    val onread: OnRead? = null
)
