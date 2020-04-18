package mqtt.socket

import mqtt.buffer.PlatformBuffer
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine
import kotlin.time.Duration

class NodeClientSocket : ClientToServerSocket {
    var netSocket: dynamic = null
    val net = require("net")

    override suspend fun open(
        timeout: Duration,
        port: UShort,
        hostname: String?,
        socketOptions: SocketOptions?
    ): SocketOptions {
        netSocket = suspendCoroutine {
            net.connect(port, hostname ?: "localhost") { socket ->
                it.resume(socket)
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
        return 1
    }

    override suspend fun close() {
        suspendCoroutine<Unit> {
            netSocket.end {
                it.resume(Unit)
            } as Unit
        }
    }
}