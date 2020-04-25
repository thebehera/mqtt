package mqtt.socket

import io.ktor.utils.io.core.internal.DangerousInternalIoApi
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.promise
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

data class DoneReading(val bytesRead: Int, val buffer: Uint8Array)

class NodeClientSocket : ClientToServerSocket {
    var netSocket: dynamic = null
    val net = require("net")

    val getReadBufferChannel = Channel<Uint8Array>()
    val getDoneReadBufferChannel = Channel<DoneReading>()

    override suspend fun open(
        timeout: Duration,
        port: UShort,
        hostname: String?,
        socketOptions: SocketOptions?
    ): SocketOptions {

        val onRead = OnRead({
            block {
                getReadBufferChannel.receive()
            }
        }, { bytesRead, buffer ->
            val string = buffer.unsafeCast<ByteArray>().decodeToString()
            console.log("Incoming $bytesRead $string ${jsTypeOf(buffer)}")
            block {
                getDoneReadBufferChannel.send(DoneReading(bytesRead, buffer))
            }
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

    @OptIn(DangerousInternalIoApi::class)
    override suspend fun read(buffer: PlatformBuffer, timeout: Duration): Int {
        getReadBufferChannel.send(Uint8Array((buffer as JsBuffer).buffer.memory.view.buffer))
        return getDoneReadBufferChannel.receive().bytesRead
    }

    @OptIn(DangerousInternalIoApi::class)
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

fun <T> block(body: suspend CoroutineScope.() -> T): dynamic = GlobalScope.promise { body() }.catch {
    if (it !is UnsupportedOperationException) {
        throw it
    }
}