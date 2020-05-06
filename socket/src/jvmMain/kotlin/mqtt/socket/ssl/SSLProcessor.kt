package mqtt.socket.ssl

import kotlinx.coroutines.sync.Mutex
import mqtt.buffer.JvmBuffer
import mqtt.buffer.PlatformBuffer
import mqtt.socket.ClientSocket
import mqtt.socket.ClientToServerSocket
import java.nio.ByteBuffer
import javax.net.ssl.SSLEngine
import javax.net.ssl.SSLEngineResult
import kotlin.time.Duration
import kotlin.time.milliseconds

class SSLProcessor (val sslEngine: SSLEngine, public val socket: ClientToServerSocket) {
    private val timeout: Duration = 5000.milliseconds
    private var networkData:ByteBuffer = ByteBuffer.allocate(sslEngine.session.packetBufferSize + 100)
    private lateinit var localData: ByteBuffer
    private val wrapMutex: Mutex = Mutex()
    private val unWrapMutex: Mutex = Mutex()

    init {
            println("SSLProcessor.init.peerHost: ${sslEngine.peerHost}, peerPort: ${sslEngine.peerPort}")
        }

    suspend fun doHandshake() {
        println("SSLProcessor.doHandshake.begin")
        socket.open(timeout, sslEngine.peerPort.toUShort(), sslEngine.peerHost)
        println("SSLProcessor.doHandshake.open done")
        localData = ByteBuffer.allocate(sslEngine.session.applicationBufferSize + 100)
        println("SSLProcessor.doHandshake: start handshake")
        sslEngine.beginHandshake()
        manageHandshake()
        println("SSLProcessor.doHandshake: end handshake")
    }
    suspend fun sslRead(buffer: PlatformBuffer) : Int{
        try {
            val jbuf: JvmBuffer = buffer as JvmBuffer
            localData = jbuf.byteBuffer
            networkData.compact()

            val enresult: SSLEngineResult = sslUnwrap(true)


            println("sslRead: $enresult")
            if (enresult.status == SSLEngineResult.Status.CLOSED || sslEngine.isInboundDone) {
                println("SSL closed by server")
                socket.close()
                return -1
            }

            return localData.remaining()
        } catch (e: Exception) {
            println("sslRead.exception: ${e.message}")
            throw e
        }
    }

    suspend fun sslWrite(buffer: PlatformBuffer) : Int {
        try {
            val jbuf: JvmBuffer = buffer as JvmBuffer

            println("sslWrite1: ${sslEngine.handshakeStatus}")
            if (sslEngine.handshakeStatus != SSLEngineResult.HandshakeStatus.NOT_HANDSHAKING) {
                println("sslWrite2a.localData: ${localData}, networkData: ${networkData}")
                sslEngine.beginHandshake()
                manageHandshake()
                println("sslWrite2: handshake done")
            }

            localData = jbuf.byteBuffer
           // localData.flip()
           // networkData.compact()
           // resetBufForRead(localData)
            val ret: Int = localData.remaining()
            val enresult: SSLEngineResult = sslWrap(true)
            //       localData.compact()
            //       networkData.compact()
            println("sslWrite3: $enresult")
            return ret
        } catch (e: Exception) {
            println("sslWrite.exception: ${e.message}")
            throw e
        }

    }

    suspend fun initiateClose() {
        var status:SSLEngineResult.Status = SSLEngineResult.Status.OK
        try {
            sslEngine.closeOutbound()
            while(! ((status == SSLEngineResult.Status.CLOSED) || sslEngine.isOutboundDone)) {
                val res: SSLEngineResult = sslWrap()
                status = res.status
            }
            socket.close()
        } catch (e: Exception) {
            throw e
        }
    }

    suspend fun receivedClose(socket: ClientSocket) {
        var status:SSLEngineResult.Status = SSLEngineResult.Status.OK
        try {
            sslEngine.closeInbound()
            socket.close()
        } catch (e: Exception) {
            println("receiveClose.exception: ${e.message}")
            throw e
        }
    }

    suspend private fun sslWrap(writeData: Boolean = true) : SSLEngineResult {
        var result: SSLEngineResult

        try {
            do {
                resetBufForRead(localData)
                resetBufForWrite(networkData)

                println("sslWrap1.localData: ${localData}, networkData: ${networkData}")
                println("sslWrap1a: ${sslEngine.handshakeStatus}")
                wrapMutex.lock()
                result = sslEngine.wrap(localData, networkData)
                wrapMutex.unlock()
                println("sslWrap2.result: ${result}, -- localData: ${localData}, networkData: ${networkData}")
                engineResultStatus(result.status, true)
                if (result.status == SSLEngineResult.Status.OK) {
                    println("sslWrap3.localData: ${localData}, networkData: ${networkData}")
                    if (writeData) {
                        val ret: Int = baseWrite(networkData)
                        if (ret < 0) {
                            println("sslWrap.write failure")
                        }
                        println("sslWrap4.ret: $ret, localData: ${localData}, networkData: ${networkData}")
                    }
                }
                if (result.handshakeStatus == SSLEngineResult.HandshakeStatus.NEED_TASK)
                    sslRunable()
            } while ((result.handshakeStatus == SSLEngineResult.HandshakeStatus.NEED_WRAP) ||
                (result.handshakeStatus == SSLEngineResult.HandshakeStatus.NEED_TASK) ||
                (result.status == SSLEngineResult.Status.BUFFER_OVERFLOW) ||
                (result.status == SSLEngineResult.Status.BUFFER_UNDERFLOW))
        } catch (e: Exception) {
            if (wrapMutex.isLocked)
                wrapMutex.unlock()
            println("sslWrap.exception: ${e.message}")
            throw e
        }
        return result
    }

    suspend private fun sslUnwrap(readData: Boolean) : SSLEngineResult {
        var result: SSLEngineResult
        var ret: Int = 0
        try {
            println("sslUnwrap1.readData: $readData, localData: ${localData}, networkData: ${networkData}")
            if (readData) {
                ret = baseRead(networkData)
                if (ret < 0)
                    println("sslUnwrap read failure")
                println("sslUnwrap3.ret: $ret, localData: ${localData}, networkData: ${networkData}")
            } else {
                resetBufForRead(networkData)
            }

            resetBufForWrite(localData)

            do {
                resetBufForRead(networkData)
                println("sslUnwrap4.localData: ${localData}; networkData: ${networkData}")
                unWrapMutex.lock()
                result = sslEngine.unwrap(networkData, localData)
                unWrapMutex.unlock()
                engineResultStatus(result.status, false)
                println("sslUnwrap4a.result: $result, localData: $localData, networkData: $networkData")
                if (result.handshakeStatus == SSLEngineResult.HandshakeStatus.NEED_TASK)
                    sslRunable()
            } while ((result.handshakeStatus == SSLEngineResult.HandshakeStatus.NEED_UNWRAP) ||
                (result.handshakeStatus == SSLEngineResult.HandshakeStatus.NEED_TASK) ||
                (result.status == SSLEngineResult.Status.BUFFER_OVERFLOW) ||
                (result.status == SSLEngineResult.Status.BUFFER_UNDERFLOW))
            println("sslUnwrap5.localData: ${localData}; networkData: ${networkData}")
            return result
        } catch (e: Exception) {
            if (unWrapMutex.isLocked)
                unWrapMutex.unlock()
            println("sslUnwrap.exception: ${e.message}")
            throw e
        }
    }

    suspend private fun sslRunable () {
        var delTask: Runnable? = sslEngine.delegatedTask
        println("sslRunnable")
        while (delTask != null) {
            Thread(delTask).start()
            delTask = sslEngine.delegatedTask
        }
    }

    suspend private fun manageHandshake() {
        var hstatus : SSLEngineResult.HandshakeStatus = sslEngine.handshakeStatus

        try {
            while (true) {
                println("manageHandshake: $hstatus")
                when (hstatus) {
                    SSLEngineResult.HandshakeStatus.FINISHED -> {
                        return
                    }
                    SSLEngineResult.HandshakeStatus.NEED_TASK -> {
                    //    sslRunable()
                        println("manageHandshake. *******should never reach here*******")
                    }
                    SSLEngineResult.HandshakeStatus.NEED_UNWRAP -> {
                        val result: SSLEngineResult = sslUnwrap(true)

                        hstatus = result.handshakeStatus
                    }
                    SSLEngineResult.HandshakeStatus.NEED_WRAP -> {
                        val result: SSLEngineResult = sslWrap()

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
        val appSize:Int = sslEngine.session.applicationBufferSize
        val availSpace: Int = if (buf.limit() == buf.capacity())  buf.limit() - buf.position() else buf.capacity() - buf.limit()
        val tBuf: ByteBuffer = ByteBuffer.allocate(appSize + availSpace + 100)
        resetBufForRead(buf)
        tBuf.put(buf)
        return tBuf
    }

    suspend private fun bufferUnderflowMemory(buf: ByteBuffer) : ByteBuffer {
        val packetSize: Int = sslEngine.session.packetBufferSize
        val availSpace: Int = if (buf.limit() == buf.capacity())  buf.limit() - buf.position() else buf.capacity() - buf.limit()
        println("bufferUnderflowMemory1.packetSize: $packetSize, availableSpace: $availSpace, capacity: ${buf.capacity()}")
        if (packetSize > availSpace) {
            val tBuf:ByteBuffer = ByteBuffer.allocate(packetSize + availSpace + 100)
            resetBufForRead(buf)
            tBuf.put(buf)
            return tBuf
        } else {
            //this should not happen for wrap status; happen only for unwrap status
            if (baseRead(buf) < 0)
                println("bufferUnderflowMemory2.read failure")
            return buf
        }
    }

    suspend private fun resetBufForRead(buf: ByteBuffer) {
        if (buf.limit() != buf.capacity()) {
            if (buf.position() != 0) {
                buf.compact()
                buf.flip()
            }
        } else {
            if (buf.position() != 0)
                buf.flip()
        }
    }

    suspend private fun resetBufForWrite(buf: ByteBuffer) {
        if (buf.limit() != buf.capacity())
            if (buf.position() != 0)
                // some data has been read and some are left to be read
                buf.compact()
            else {
                // buf is inread status and some data to be read
                // chagne the pos * limit pointers to read additional data
                buf.position(buf.limit())
                buf.limit(buf.capacity())
            }
    }

    // reads data from the network into buf.
    suspend private fun baseRead(buf: ByteBuffer) : Int {
        try {
            resetBufForWrite(buf)

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

    suspend private fun baseWrite(buf: ByteBuffer) : Int {
        var count: Int = 0
        var sendCount: Int = 0
        try {
           resetBufForWrite(buf)
            println("baseWrite1: $buf")
            while (buf.hasRemaining()) {
                val r: JvmBuffer = JvmBuffer(buf)
                println("baseWrite.jvmBuffer: $r")
                val ret:Int = socket.write(r, timeout)
                println("baseWrite2.ret: $ret, buf: $buf")
                if (ret < 0) {
                    println("baseWrite3: socket error")
                    return -1
                } else if (ret == 0) {
                    count++
                    if (count > 10) {
                        println("baseWrite4. unable to send data")
                        return -2
                    }
                    sendCount += ret
                }
            }
            return sendCount
        } catch (e: Exception) {
            println("baseWrite.exception: ${e.message}")
            throw e
        }
    }
}