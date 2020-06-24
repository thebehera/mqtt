package mqtt.socket

import org.khronos.webgl.Uint8Array
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.coroutines.suspendCoroutine


suspend fun connect(tcpOptions: tcpOptions): Socket {
    var netSocket: Socket? = null
    suspendCoroutine<Unit> {
        val socket = Net.connect(tcpOptions) {
            it.resume(Unit)
        }
        socket.on("error") { e ->
            it.resumeWithException(RuntimeException(e.toString()))
        }
        netSocket = socket
    }
    return netSocket!!
}

suspend fun Socket.write(buffer: Uint8Array) {
    suspendCoroutine<Unit> {
        write(buffer) {
            it.resume(Unit)
        }
    }
}

suspend fun Socket.close() {
    suspendCoroutine<Unit> {
        end {
            it.resume(Unit)
        }
    }
}