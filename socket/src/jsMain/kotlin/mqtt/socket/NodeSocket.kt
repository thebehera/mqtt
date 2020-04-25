package mqtt.socket

import mqtt.buffer.JsBuffer
import mqtt.buffer.PlatformBuffer
import org.khronos.webgl.Uint8Array
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.coroutines.suspendCoroutine
import kotlin.time.Duration

class OnRead(val buffer: () -> Uint8Array, val callback: (Int, Uint8Array) -> Boolean)

class tcpOptions(
    val port: Int,
    val host: String? = null,
    val onread: OnRead
)


class NodeClientSocket : ClientToServerSocket {
    var netSocket: dynamic = null
    val net = require("net")

    override suspend fun open(
        timeout: Duration,
        port: UShort,
        hostname: String?,
        socketOptions: SocketOptions?
    ): SocketOptions {
        val onRead = OnRead({
            Uint8Array(13)
        }, { bytesRead, buffer ->
            val string = buffer.unsafeCast<ByteArray>().decodeToString()
            console.log("Incoming $bytesRead $string ${jsTypeOf(buffer)}")
            true
        })
        netSocket = suspendCoroutine {
            val socket = net.connect(tcpOptions(port.toInt(), hostname, onRead)) { socket ->
                it.resume(socket)
            }
            socket.on("error") { e ->
                it.resumeWithException(RuntimeException(e.toString()))
            } as Unit
        }
        return SocketOptions()
    }

    override fun isOpen(): Boolean {
        return try {
            netSocket!!.remoteAddress is String
        } catch (t: Throwable) {
            false
        }
    }

    override fun localPort(): UShort? {
        return netSocket.localPort as? UShort
    }

    override fun remotePort(): UShort? {
        return netSocket.remotePort as? UShort
    }

    override suspend fun read(buffer: PlatformBuffer, timeout: Duration): Int {
        return 0
    }

    override suspend fun write(buffer: PlatformBuffer, timeout: Duration): Int {
        val array = Uint8Array((buffer as JsBuffer).buffer.memory.view.buffer)
        suspendCoroutine<Unit> {
            netSocket.write(array) {
                it.resume(Unit)
            } as Unit
        }
        return array.byteLength
    }

    override suspend fun close() {
        suspendCoroutine<Unit> {
            netSocket.end {
                it.resume(Unit)
            } as Unit
        }
    }
}
