package mqtt.socket.ssl

import mqtt.socket.ClientToServerSocket
import java.io.FileInputStream
import java.security.KeyStore
import javax.net.ssl.KeyManagerFactory
import javax.net.ssl.SSLContext
import javax.net.ssl.SSLEngine
import javax.net.ssl.TrustManagerFactory

enum class SSLVersion (val version: String){
    TLS ("TLS"), TLSv1("TSLv1"), TLSv1_1("TLSv1.1"), TLSv1_2("TLSv1.2")
}

class SSLManager (val trustStore: String, val trustPassword: String, val keyStore: String, val keyPassword: String,
                    val sslVersion: SSLVersion = SSLVersion.TLSv1_2) {
    data class Engine (val engine: SSLEngine, var count: UShort = 1u)

    val ctx: SSLContext = SSLContext.getInstance(sslVersion.version)
    val engineMap: HashMap<String, Engine> = hashMapOf<String, Engine>()

    init {
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
        ctx.init(null, null, null)
    }

    suspend fun getSSLclient(socket: ClientToServerSocket, host: String, port: Int) : SSLProcessor {
        val key: String = host + ":" + port
        val engine: Engine? = engineMap.get(key)

        if (engine != null) {
            engine.count++
            engineMap.put(key, engine)
            return SSLProcessor(engine.engine, socket)
        } else {
            val enginex: SSLEngine = ctx.createSSLEngine(host, port)
            enginex.useClientMode = true
            val x: Engine = Engine(enginex)
            engineMap.put(key, x)
            val sslPro: SSLProcessor = SSLProcessor(enginex, socket)
            sslPro.doHandshake()
            return sslPro
        }
    }

}