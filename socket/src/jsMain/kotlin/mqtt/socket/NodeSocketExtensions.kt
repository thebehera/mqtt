package mqtt.socket

import org.khronos.webgl.Uint8Array
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.coroutines.suspendCoroutine


suspend fun connect(tcpOptions: tcpOptions): Socket {
    var netSocket: Socket? = null
    suspendCoroutine<Unit> {
        var count = 0
        val socket = Net.connect(tcpOptions) {
            ++count
            it.resume(Unit)
        }
        socket.on("error") { e ->
            if (count == 0) {
                it.resumeWithException(RuntimeException(e.toString()))
            } else {
                console.log("error with connection", e)
            }
        }
        netSocket = socket
    }
    return netSocket!!
}

suspend fun connect(tcpOptions: TcpSocketConnectOpts): Socket {
    var netSocket: Socket? = null
    suspendCoroutine<Unit> {
        var count = 0
        val socket = Net.connect(tcpOptions) {
            println("resume ${++count}")
            it.resume(Unit)
        }
        socket.on("error") { e ->
            println("resume ${++count}")
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