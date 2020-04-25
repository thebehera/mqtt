package jvmMain.kotlin.mqtt.socket.ssl

import mqtt.buffer.JvmBuffer
import mqtt.buffer.PlatformBuffer
import mqtt.socket.ClientSocket
import mqtt.socket.ClientToServerSocket
import java.nio.ByteBuffer
import kotlinx.coroutines.sync.Mutex
import java.io.FileInputStream
import java.security.KeyStore
import javax.net.ssl.*
import kotlin.time.Duration
import kotlin.time.ExperimentalTime
import kotlin.time.milliseconds

class SSLClient {
    private lateinit var ctx : SSLContext
    private lateinit var clientEngine: SSLEngine
    @ExperimentalTime
    private val timeout: Duration = 5000.milliseconds
    private val wrapMutex: Mutex = Mutex()
    private val unWrapMutex: Mutex = Mutex()
    private lateinit var localData: ByteBuffer
    private lateinit var networkData: ByteBuffer
    @ExperimentalTime
    private lateinit var socket: ClientSocket

//    data class ResultStatusBuf (var src: ByteBuffer, var dest: ByteBuffer)

    @ExperimentalUnsignedTypes
    @ExperimentalTime
    suspend fun open(socket: ClientToServerSocket, host: String, port: UShort) {

        try {
            sslSetup()
            socket.open(timeout, port, host)
            println("open ${socket.isOpen()}")
            this.socket = socket
            clientEngine.beginHandshake()
            println("starting handshake")
            manageHandshake()
            println("handshake done")
        } catch (e: Exception) {
            println("SSLClient.open.exception: ${e.message}")
            throw e
        }
    }

    @ExperimentalUnsignedTypes
    @ExperimentalTime
    suspend fun sslRead(buffer: PlatformBuffer) : Int{
        try {
            val jbuf: JvmBuffer = buffer as JvmBuffer
            localData = jbuf.byteBuffer
            networkData.compact()
            val nBuf: JvmBuffer = JvmBuffer(networkData)
            if (socket.read(nBuf, timeout) < 0)
                println("sslRead failed")
            networkData.flip()
            localData.compact()

            val enresult: SSLEngineResult = sslUnwrap(true)
            if (enresult.status == SSLEngineResult.Status.CLOSED || clientEngine.isInboundDone) {
                println("SSL closed by server")
                socket.close()
                return -1
            }
            val x:Int = localData.limit() - localData.position()

            return x
        } catch (e: Exception) {
            println("sslRead.exception: ${e.message}")
            throw e
        }
    }

    @ExperimentalUnsignedTypes
    @ExperimentalTime
    suspend fun sslWrite(buffer: PlatformBuffer) : Int {
        try {
            val jbuf: JvmBuffer = buffer as JvmBuffer
            localData = jbuf.byteBuffer
            localData.flip()
            networkData.compact()
            val enresult: SSLEngineResult = sslWrap()
            localData.compact()
            networkData.compact()
            return localData.limit()
        } catch (e: Exception) {
            println("sslWrite.exception: ${e.message}")
            throw e
        }

    }

    @ExperimentalTime
    suspend fun initiateClose() {
        var status:SSLEngineResult.Status = SSLEngineResult.Status.OK
        try {
            clientEngine.closeOutbound()
            while(! ((status == SSLEngineResult.Status.CLOSED) || clientEngine.isOutboundDone)) {
                val res: SSLEngineResult = sslWrap()
                status = res.status
            }
            socket.close()
        } catch (e: Exception) {
            throw e
        }
    }

    @ExperimentalTime
    suspend fun receivedClose(socket: ClientSocket) {
        var status:SSLEngineResult.Status = SSLEngineResult.Status.OK
        try {
            clientEngine.closeInbound()
            socket.close()
        } catch (e: Exception) {
            println("receiveClose.exception: ${e.message}")
            throw e
        }
    }

    @ExperimentalUnsignedTypes
    @ExperimentalTime
    suspend private fun sslWrap() : SSLEngineResult {
        val result: SSLEngineResult

        try {
            if (localData.position() != 0)
                localData.flip()
            if (networkData.position() != 0)
                networkData.compact()
            println("sslWrap1.localData: ${localData}, networkData: ${networkData}")
            wrapMutex.lock()
            result = clientEngine.wrap(localData, networkData)
            wrapMutex.unlock()
            println("sslWrap2.localData: ${localData}, networkData: ${networkData}")
            engineResultStatus(result.status, true)
            if (result.status == SSLEngineResult.Status.OK) {
                println("sslWrap3.networkData: ${networkData}")
                val r: JvmBuffer = JvmBuffer(networkData)
                println("sslWrap4.r: $r")
                val ret:Int = socket.write(r, timeout)
                println("sslWrap5.written data size: $ret")
                if (ret < 0) {
                    println("sslWrap.write failure")
                }
                println("sslWrap6.networkData: ${networkData}")
            }
        } catch (e: Exception) {
            println("sslWrap.exception: ${e.message}")
            throw e
        }
        return result
    }

    @ExperimentalUnsignedTypes
    @ExperimentalTime
    suspend private fun sslUnwrap(readable: Boolean) : SSLEngineResult {
        val result: SSLEngineResult
        var ret: Int = 0
        try {
            if ((networkData.position() != 0) && (networkData.capacity() != networkData.limit()))
                networkData.compact()
            println("sslUnwrap1.localData: ${localData}, networkData: ${networkData}")
            if (readable) {
                val x: PlatformBuffer = JvmBuffer(networkData)
                ret = socket.read(x, timeout)
            }
            if (ret < 0){
                println("sslUnwrap read failure")
            }

            if (localData.position() != 0)
                localData.compact()
            if (networkData.position() != 0)
                networkData.flip()
            println("sslUnwrap2.ret: $ret, localData: ${localData}; networkData: ${networkData}")
            unWrapMutex.lock()
            result = clientEngine.unwrap(networkData, localData)
            unWrapMutex.unlock()
            engineResultStatus(result.status, false)
            println("sslUnwrap3.localData: ${localData}; networkData: ${networkData}")
            return result
        } catch (e: Exception) {
            println("sslUnwrap.exception: ${e.message}")
            throw e
        }
    }

    suspend private fun sslRunable () {
        var delTask: Runnable? = clientEngine.delegatedTask

        while (delTask != null) {
            Thread(delTask).start()
            delTask = clientEngine.delegatedTask
        }
    }

    @ExperimentalUnsignedTypes
    @ExperimentalTime
    suspend private fun manageHandshake() {
        var hstatus : SSLEngineResult.HandshakeStatus = clientEngine.handshakeStatus
        var pstatus: SSLEngineResult.HandshakeStatus = hstatus

        var x: Int = 0
        try {
            while (true) {
                println("manageHandshake: $hstatus")
                x++
                if (x >= 100)
                    return
                when (hstatus) {
                    SSLEngineResult.HandshakeStatus.FINISHED -> {
                        return
                    }
                    SSLEngineResult.HandshakeStatus.NEED_TASK -> {
                        sslRunable()

                        if (pstatus == SSLEngineResult.HandshakeStatus.NEED_UNWRAP) {
                            hstatus = clientEngine.unwrap(networkData, localData).handshakeStatus
                            println("manageHandshake.localData: ${localData}, networkData: ${networkData}")
                        }
                      //  else if (pstatus == SSLEngineResult.HandshakeStatus.NEED_WRAP) { }
                     //   println("manageHandshake.localData: ${localData}, networkData: ${networkData}")
                       // hstatus = pstatus
                    }
                    SSLEngineResult.HandshakeStatus.NEED_UNWRAP -> {
                        val result: SSLEngineResult = sslUnwrap(!(pstatus == hstatus))
                        pstatus = hstatus
                        hstatus = result.handshakeStatus
                    }
                    SSLEngineResult.HandshakeStatus.NEED_WRAP -> {
                        val result: SSLEngineResult = sslWrap()
                        pstatus = hstatus
                        hstatus = result.handshakeStatus
                    }
                    SSLEngineResult.HandshakeStatus.NOT_HANDSHAKING -> {
                        println("manageHandshake.not_handshaking")
                        return
                    }
                }
            }
        } catch (e: Exception) {
            println("manageHandshake.exception: ${e.message}")
            throw e
        }
    }

    @ExperimentalTime
    @ExperimentalUnsignedTypes
    suspend private fun engineResultStatus(status: SSLEngineResult.Status, wrap: Boolean) {
        println("engineResultStatus: $status, $wrap")
        when (status) {
            SSLEngineResult.Status.BUFFER_OVERFLOW -> {
                //not enough capacity in dest ByteBuffer
                if (wrap)
                    networkData = bufferOverflowMemory(networkData)
                else
                    localData = bufferOverflowMemory(localData)
                println("engineResultStatus.OverFlow.localData: ${localData}, networkData: ${networkData}")
                return
            }
            SSLEngineResult.Status.BUFFER_UNDERFLOW -> {
                // not enough capacity in the src ByteBuffer
                if (wrap)
                    localData = bufferUnderflowMemory(localData)
                else
                    networkData = bufferUnderflowMemory(networkData)
                println("engineResultStatus.UnderFlow.localData: ${localData}, networkData: ${networkData}")
                return
            }
            SSLEngineResult.Status.CLOSED -> {
                // SSLengine just got closed or was closed
                return
            }
            SSLEngineResult.Status.OK -> {
                // SSLengine successfully completed the operation
                return
            }
        }
    }

    suspend private fun bufferOverflowMemory(buf: ByteBuffer) : ByteBuffer {
        val x:Int = clientEngine.session.applicationBufferSize
        val tBuf: ByteBuffer = ByteBuffer.allocate(x + buf.position() + 100)
        if (buf.position() != 0)
            buf.flip()
        tBuf.put(buf)
        return tBuf
    }

    @ExperimentalUnsignedTypes
    @ExperimentalTime
    suspend private fun bufferUnderflowMemory(buf: ByteBuffer) : ByteBuffer {
        val x: Int = clientEngine.session.packetBufferSize
        val availSpace: Int = buf.limit() - buf.position()
        println("bufferUnderflowMemory: $x, $availSpace, ${buf.capacity()}")
        if (x > availSpace) {
            val tBuf:ByteBuffer = ByteBuffer.allocate(x + availSpace + 100)
            if (buf.position() != 0)
                buf.flip()
            tBuf.put(buf)
            return tBuf
        } else {
            //this should not happen for wrap status; happen only for unwrap status
            if (buf.position() != 0)
                buf.compact()
            val jbuf: JvmBuffer = JvmBuffer(buf)
            if (socket.read(jbuf, timeout) < 0) {
                println("bufferUnderflowMemory read failed")
            }
            return buf
        }
    }

    suspend private fun sslSetup() {
        try {
            val passPhrase: CharArray = "changeit".toCharArray()
            val trustStore: KeyStore = KeyStore.getInstance("JKS")
            val keyStore: KeyStore = KeyStore.getInstance("JKS")
            trustStore.load(FileInputStream("/Users/sbehera/cacerts"), passPhrase)
            keyStore.load(FileInputStream("/Users/sbehera/cacerts"), passPhrase)
            val tMf: TrustManagerFactory = TrustManagerFactory.getInstance("SunX509")
            val kMf: KeyManagerFactory = KeyManagerFactory.getInstance("SunX509")

            tMf.init(trustStore)
            kMf.init(keyStore, passPhrase)
            ctx = SSLContext.getInstance("TLSv1")
            ctx.init(kMf.keyManagers, tMf.trustManagers, null)
            clientEngine = ctx.createSSLEngine("www.google.com", 443)
            clientEngine.useClientMode = true

            val sess: SSLSession = clientEngine.session
            println("sslSetup.cypherSuites: ${clientEngine.supportedCipherSuites.size}")
            localData = ByteBuffer.allocate(sess.applicationBufferSize + 100)
            networkData = ByteBuffer.allocate(sess.packetBufferSize + 100)
        } catch (e: Exception) {
            println("sslSetup.exception: ${e.message}")
            throw e
        }
    }
}