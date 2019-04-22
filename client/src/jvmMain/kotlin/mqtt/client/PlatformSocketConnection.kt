package mqtt.client

import io.ktor.network.selector.ActorSelectorManager
import io.ktor.network.sockets.aSocket
import io.ktor.network.tls.tls
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import java.security.cert.X509Certificate
import javax.net.ssl.X509TrustManager

actual class PlatformSocketConnection actual constructor(override val parameters: ConnectionParameters)
    : SocketSession() {
    override val dispatcher: CoroutineDispatcher = Dispatchers.IO

    override suspend fun buildSocket(): PlatformSocket {
        @Suppress("EXPERIMENTAL_API_USAGE")
        val socketBuilder = aSocket(ActorSelectorManager(Dispatchers.IO)).tcp()
        val tmpSocketRef = socketBuilder.connect(parameters.hostname, parameters.port)
        val socket = if (parameters.secure) {
            tmpSocketRef.tls(coroutineContext) {
                if (parameters.acceptAllCertificates) {
                    trustManager = object : X509TrustManager {
                        override fun checkClientTrusted(p0: Array<out X509Certificate>?, p1: String?) {}
                        override fun checkServerTrusted(p0: Array<out X509Certificate>?, p1: String?) {}
                        override fun getAcceptedIssuers(): Array<X509Certificate> {
                            return emptyArray()
                        }
                    }
                }
            }
        } else {
            tmpSocketRef
        }
        ShutdownHook.shutdownThread.addConnection(this)
        return JavaPlatformSocket(socket)
    }

    override fun beforeClosingSocket() {
        ShutdownHook.shutdownThread.removeConnections(this)
    }
}