package mqtt.socket.nio2

import mqtt.buffer.BufferPool
import mqtt.socket.ClientToServerSocket
import mqtt.socket.SocketOptions
import mqtt.socket.nio.util.asInetAddress
import mqtt.socket.nio.util.asyncSetOptions
import mqtt.socket.nio2.util.aConnect
import mqtt.socket.nio2.util.asyncSocket
import java.net.InetAddress
import java.net.InetSocketAddress
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.coroutines.suspendCoroutine
import kotlin.time.Duration
import kotlin.time.ExperimentalTime

@ExperimentalUnsignedTypes
@ExperimentalTime
class AsyncClientSocket(pool: BufferPool) : AsyncBaseClientSocket(pool), ClientToServerSocket {

    override suspend fun open(
        timeout: Duration,
        port: UShort,
        hostname: String?,
        socketOptions: SocketOptions?
    ): SocketOptions {
        val socketAddress = if (hostname != null) {
            InetSocketAddress(hostname.asInetAddress(), port.toInt())
        } else {
            suspendCoroutine {
                try {
                    it.resume(InetSocketAddress(InetAddress.getLocalHost(), port.toInt()))
                } catch (e: Exception) {
                    it.resumeWithException(e)
                }
            }
        }
        val asyncSocket = asyncSocket()
        this.socket = asyncSocket
        val options = asyncSocket.asyncSetOptions(socketOptions)
        asyncSocket.aConnect(socketAddress)
        return options
    }

}


