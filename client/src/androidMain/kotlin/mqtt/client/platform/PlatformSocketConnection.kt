@file:Suppress("EXPERIMENTAL_API_USAGE", "EXPERIMENTAL_OVERRIDE")

package mqtt.client.platform

import io.ktor.network.selector.ActorSelectorManager
import io.ktor.network.sockets.aSocket
import io.ktor.network.tls.tls
import kotlinx.coroutines.Dispatchers
import mqtt.client.JavaSocketTransport
import mqtt.client.transport.SocketTransport
import mqtt.client.transport.Transport
import mqtt.connection.IRemoteHost
import java.security.cert.X509Certificate
import javax.net.ssl.X509TrustManager
import kotlin.coroutines.CoroutineContext

actual class PlatformSocketConnection actual constructor(
    override val remoteHost: IRemoteHost,
    ctx: CoroutineContext
) : SocketTransport(ctx) {

    override val supportsNativeSockets = true

    override suspend fun buildNativeSocket(): Transport {
        @Suppress("EXPERIMENTAL_API_USAGE")
        val socketBuilder = aSocket(ActorSelectorManager(Dispatchers.IO)).tcp()
        val tmpSocketRef = socketBuilder.connect(hostname = remoteHost.name, port = remoteHost.port.toInt())
        val socket = if (remoteHost.security.isTransportLayerSecurityEnabled) {
            tmpSocketRef.tls(coroutineContext) {
                if (remoteHost.security.acceptAllCertificates) {
                    trustManager = object : X509TrustManager {
                        override fun checkClientTrusted(p0: Array<out X509Certificate>?, p1: String?) {}
                        override fun checkServerTrusted(p0: Array<out X509Certificate>?, p1: String?) {}
                        override fun getAcceptedIssuers(): Array<X509Certificate> {
                            return emptyArray()
                        }
                    }
                }
            }
            tmpSocketRef
        } else {
            tmpSocketRef
        }
        return JavaSocketTransport(socket)
    }

}
