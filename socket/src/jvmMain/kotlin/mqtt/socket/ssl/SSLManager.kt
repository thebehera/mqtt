package mqtt.socket.ssl


import mqtt.buffer.BufferPool
import java.nio.channels.AsynchronousSocketChannel
import javax.net.ssl.SSLContext

enum class SSLVersion (val version: String){
    DEFAULT("DEFAULT"), TLS ("TLS"), TLSv1("TSLv1"), TLSv1_1("TLSv1.1"), TLSv1_2("TLSv1.2")
}

//class SSLManager (val trustStore: String, val trustPassword: String, val keyStore: String, val keyPassword: String,
//                 val sslVersion: SSLVersion = SSLVersion.TLSv1_2) {
object SSLManager {
//    data class ConnectionCount (val sslEngine: SSLEngine, var count: UShort = 1u)
    private var sslCtx: SSLContext? = null
    private val connectionCountMap: HashMap<String, UShort> = hashMapOf<String, UShort>()


    suspend fun getSSLclient(pool: BufferPool, socket: AsynchronousSocketChannel, host: String,
                             port: UShort, sslVersion: SSLVersion = SSLVersion.DEFAULT) : SSLProcessor {
        val key: String = host + ":" + port
        var count: UShort? = connectionCountMap.get(key)

        val ctx = getCtx(sslVersion)
        val sslEngine = ctx.createSSLEngine(host, port.toInt())
        val sslProcessor = SSLProcessor(pool, sslEngine, socket)

        if (count != null) {
            connectionCountMap.put(key, ++count)
        } else {
            sslEngine.useClientMode = true
            sslEngine.needClientAuth = false
            connectionCountMap.put(key, 1u)

            sslProcessor.doHandshake()
        }

        return sslProcessor
    }

    private suspend fun getCtx (sslVersion: SSLVersion = SSLVersion.DEFAULT) : SSLContext {
        if (sslCtx != null)
            return sslCtx!!

        sslCtx = if (sslVersion == SSLVersion.DEFAULT) SSLContext.getDefault() else SSLContext.getInstance(sslVersion.version)
/*        val trustPassPhrase: CharArray = trustPassword.toCharArray()
        val keyPassPhrase: CharArray = keyPassword.toCharArray()
        val trustStoreX: KeyStore = KeyStore.getInstance("JKS")
        val keyStoreX: KeyStore = KeyStore.getInstance("JKS")
        trustStoreX.load(FileInputStream(trustStore), trustPassPhrase)
        keyStoreX.load(FileInputStream(keyStore), keyPassPhrase)
        val tMf: TrustManagerFactory = TrustManagerFactory.getInstance("SunX509")
        val kMf: KeyManagerFactory = KeyManagerFactory.getInstance("SunX509")
        tMf.init(trustStoreX)
        kMf.init(keyStoreX, keyPassPhrase)
        ctx.init(kMf.keyManagers, tMf.trustManagers, null)
*/
//        sslCtx!!.init(null, null, null)

        return sslCtx!!
    }

}
