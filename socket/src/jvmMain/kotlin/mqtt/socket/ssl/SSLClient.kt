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
     //       val nBuf: JvmBuffer = JvmBuffer(networkData)
     //       if (socket.read(nBuf, timeout) < 0)
     //           println("sslRead failed")
     //       networkData.flip()
     //       localData.compact()

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
            val ret: Int = localData.remaining()
            val enresult: SSLEngineResult = sslWrap()
     //       localData.compact()
     //       networkData.compact()
            return ret
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
            if ((localData.position() != 0) && (localData.limit() != localData.capacity()))
                localData.compact()
            if (localData.position() != 0)
                localData.flip()
            if ((networkData.position() != 0) && (networkData.limit() != networkData.capacity()))
                networkData.compact()
            println("sslWrap1.localData: ${localData}, networkData: ${networkData}")
            wrapMutex.lock()
            result = clientEngine.wrap(localData, networkData)
            wrapMutex.unlock()
            println("sslWrap2.localData: ${localData}, networkData: ${networkData}")
            engineResultStatus(result.status, true)
            if (result.status == SSLEngineResult.Status.OK) {
                println("sslWrap3.localData: ${localData}, networkData: ${networkData}")
                val r: JvmBuffer = JvmBuffer(networkData)
                val ret:Int = socket.write(r, timeout)
                if (ret < 0) {
                    println("sslWrap.write failure")
                }
                println("sslWrap4.ret: $ret, localData: ${localData}, networkData: ${networkData}")
            }
        } catch (e: Exception) {
            println("sslWrap.exception: ${e.message}")
            throw e
        }
        return result
    }

    @ExperimentalUnsignedTypes
    @ExperimentalTime
    suspend private fun sslUnwrap(readData: Boolean) : SSLEngineResult {
        val result: SSLEngineResult
        var ret: Int = 0
        try {
            println("sslUnwrap1.readData: $readData, localData: ${localData}, networkData: ${networkData}")
            if (readData) {
                ret = baseRead(networkData)
                if (ret < 0)
                    println("sslUnwrap read failure")
                println("sslUnwrap3.ret: $ret, localData: ${localData}, networkData: ${networkData}")
            } else {
                if ((networkData.position() != 0) && (networkData.capacity() != networkData.limit()))
                    networkData.compact()
                if (networkData.position() != 0)
                    networkData.flip()
            }

            if ((localData.position() != 0) && (localData.limit() != localData.capacity()))
                localData.compact()

            println("sslUnwrap4.localData: ${localData}; networkData: ${networkData}")
            unWrapMutex.lock()
            result = clientEngine.unwrap(networkData, localData)
            unWrapMutex.unlock()
            engineResultStatus(result.status, false)
            println("sslUnwrap5.localData: ${localData}; networkData: ${networkData}")
            return result
        } catch (e: Exception) {
            println("sslUnwrap.exception: ${e.message}")
            throw e
        }
    }

    suspend private fun sslRunable () {
        var delTask: Runnable? = clientEngine.delegatedTask
        println("sslRunnable")
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
                        hstatus = pstatus
                    }
                    SSLEngineResult.HandshakeStatus.NEED_UNWRAP -> {
                        var result: SSLEngineResult = sslUnwrap(true)
                        while ((result.handshakeStatus == SSLEngineResult.HandshakeStatus.NEED_TASK) ||
                               (result.handshakeStatus == SSLEngineResult.HandshakeStatus.NEED_UNWRAP)){
                            if (result.handshakeStatus == SSLEngineResult.HandshakeStatus.NEED_TASK)
                                sslRunable()
                            result = sslUnwrap(false)
                        }
                        pstatus = hstatus
                        hstatus = result.handshakeStatus
                    }
                    SSLEngineResult.HandshakeStatus.NEED_WRAP -> {
                        var result: SSLEngineResult = sslWrap()
                        while (result.handshakeStatus == SSLEngineResult.HandshakeStatus.NEED_TASK) {
                            sslRunable()
                            result = sslWrap()
                        }
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
        val availSpace: Int = buf.capacity() - buf.limit()
        println("bufferUnderflowMemory1: $x, $availSpace, ${buf.capacity()}")
        if (x > availSpace) {
            val tBuf:ByteBuffer = ByteBuffer.allocate(x + availSpace + 100)
            if (buf.position() != 0)
                buf.flip()
            tBuf.put(buf)
            return tBuf
        } else {
            //this should not happen for wrap status; happen only for unwrap status
            if (baseRead(buf) < 0)
                println("bufferUnderflowMemory2.read failure")
            return buf
        }
    }

    suspend private fun baseRead(buf: ByteBuffer) : Int {
        try {
            if (buf.limit() != buf.capacity())
                if (buf.position() == 0) {
                    //buf is in read status with some data to be read
                    //change the pos & limit pointers to read additional data
                    buf.position(buf.limit())
                    buf.limit(buf.capacity())
                } else {
                    //some data has been read and some are left to be read
                    buf.compact()
                }

            val jBuf: JvmBuffer = JvmBuffer(buf)
            println("baseRead1.jvmBuf: ${jBuf}")
            val ret: Int = socket.read(jBuf, timeout)
            println("baseRead2.read: $ret, jvmBuf: ${jBuf}")

            return ret
        } catch (e: Exception) {
            println("baseRead.exception: ${e.message}")
            throw e
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

            System.setProperty("javax.net.debug", "all")
            ctx = SSLContext.getInstance("TLSv1.2")
            ctx.init(kMf.keyManagers, tMf.trustManagers, null)
            clientEngine = ctx.createSSLEngine("controlcenter.centurylink.com", 443)
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