@file:OptIn(DangerousInternalIoApi::class)
package mqtt.socket

import io.ktor.utils.io.core.internal.DangerousInternalIoApi
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.launch
import mqtt.buffer.BufferMemoryLimit
import mqtt.buffer.BufferPool
import mqtt.buffer.JsBuffer
import mqtt.buffer.PlatformBuffer
import org.khronos.webgl.Uint8Array
import kotlin.coroutines.coroutineContext
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.coroutines.suspendCoroutine
import kotlin.time.Duration

class NodeClientSocket : ClientToServerSocket {
    var netSocket: Socket? = null
    val pool = BufferPool(limits = object : BufferMemoryLimit {
        override fun isTooLargeForMemory(size: UInt) = false
    })
    val incomingMessageChannel = Channel<Uint8Array>()

    override suspend fun open(
        timeout: Duration,
        port: UShort,
        hostname: String?,
        socketOptions: SocketOptions?
    ): SocketOptions {
        val ctx = CoroutineScope(coroutineContext)
        val onRead = OnRead({
            val b = Uint8Array((pool.borrow(520u) as JsBuffer).buffer.memory.view.buffer)
            b
        }, { _, buffer ->
            ctx.launch {
                incomingMessageChannel.send(buffer)
            }
            true
        })
        console.log(tcpOptions(port.toInt(), hostname, onRead))
        suspendCoroutine<Socket> {
            val socket = netRef!!.connect(tcpOptions(port.toInt(), hostname, onRead)) { socket ->
                it.resume(socket)
            }
            netSocket = socket
            socket.on("error") { e ->
                it.resumeWithException(RuntimeException(e.toString()))
            }
        }
        console.log("$netSocket local: ${netSocket?.localPort} ${netSocket?.remoteAddress}:${netSocket?.remotePort}")
        return SocketOptions()
    }

    override fun isOpen(): Boolean {
        return try {
            netSocket?.remoteAddress is String
        } catch (t: Throwable) {
            false
        }
    }

    override fun localPort() = netSocket?.localPort?.toUShort()

    override fun remotePort() = netSocket?.remotePort?.toUShort()

    override suspend fun read(buffer: PlatformBuffer, timeout: Duration): Int {
        val recvBuffer = incomingMessageChannel.receive()
        return recvBuffer.byteLength
    }

    override suspend fun write(buffer: PlatformBuffer, timeout: Duration): Int {
        val array = Uint8Array((buffer as JsBuffer).buffer.memory.view.buffer)
        val netSocket = netSocket ?: return 0
        suspendCoroutine<Unit> {
            netSocket.write(array) {
                it.resume(Unit)
            }
        }
        return array.byteLength
    }

    override suspend fun close() {
        suspendCoroutine<Unit> {
            netSocket?.end {
                it.resume(Unit)
            } as Any
        }
    }
}
