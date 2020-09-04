package mqtt.socket.nio2

import mqtt.buffer.BufferPool
import mqtt.socket.ClientToServerSocket
import mqtt.socket.SocketOptions
import mqtt.socket.nio.util.asInetAddress
import mqtt.socket.nio.util.asyncSetOptions
import mqtt.socket.nio2.util.aConnect
import mqtt.socket.nio2.util.asyncSocket
import mqtt.socket.ssl.SSLManager
import mqtt.socket.ssl.SSLVersion
import java.net.InetAddress
import java.net.InetSocketAddress
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.coroutines.suspendCoroutine
import kotlin.time.Duration
import kotlin.time.ExperimentalTime

@ExperimentalUnsignedTypes
@ExperimentalTime
class AsyncClientSocket(private val pool: BufferPool, private val ssl: Boolean) : AsyncBaseClientSocket(pool, ssl), ClientToServerSocket {

    override suspend fun open(
        port: UShort,
        timeout: Duration,
        hostname: String,
        socketOptions: SocketOptions?
    ): SocketOptions {
        val socketAddress = if (hostname.compareTo("lcoalhost") != 0) {
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

        if (ssl)
            sslProcess = SSLManager.getSSLclient(pool, asyncSocket, hostname, port, SSLVersion.DEFAULT)

        return options
    }
}


