package mqtt.socket.ssl

import kotlinx.coroutines.sync.Mutex
import mqtt.buffer.BufferPool
import mqtt.buffer.JvmBuffer
import mqtt.buffer.PlatformBuffer
import mqtt.socket.ClientSocket
import mqtt.socket.ClientToServerSocket
import mqtt.socket.SocketDataRead
import java.nio.ByteBuffer
import javax.net.ssl.SSLEngine
import javax.net.ssl.SSLEngineResult
import kotlin.time.Duration
import kotlin.time.milliseconds

class SSLProcessor (private val sslEngine: SSLEngine, public val socket: ClientToServerSocket) {
    private val timeout: Duration = 5000.milliseconds
    private lateinit var networkData: ByteBuffer
    private lateinit var localData: ByteBuffer
    private val wrapMutex: Mutex = Mutex()
    private val unWrapMutex: Mutex = Mutex()
    private val bufferPool = BufferPool()

    init {
        networkData = (bufferPool.borrowAsync((sslEngine.session.packetBufferSize.toFloat() * 1.5).toUInt()) as JvmBuffer).byteBuffer
        localData = (bufferPool.borrowAsync((sslEngine.session.applicationBufferSize.toFloat() * 1.5).toUInt()) as JvmBuffer).byteBuffer

        println("SSLProcessor.init.peerHost: ${sslEngine.peerHost}, peerPort: ${sslEngine.peerPort}")
    }

    suspend fun doHandshake() {
        println("SSLProcessor.doHandshake.begin")
        socket.open(timeout, sslEngine.peerPort.toUShort(), sslEngine.peerHost)
        println("SSLProcessor.doHandshake.open done")
        println("SSLProcessor.doHandshake: start handshake")
        sslEngine.beginHandshake()
        manageHandshake()
        println("SSLProcessor.doHandshake: end handshake")
    }
    suspend fun sslRead(buffer: PlatformBuffer) : Int {
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
            var ret: Int
            var enresult: SSLEngineResult
            var handshake: Boolean

            println("sslWrite1: ${sslEngine.handshakeStatus}")

            do {
                handshake = false
                localData = jbuf.byteBuffer
                ret = localData.remaining()
                enresult = sslWrap(true)
                println("sslWrite3: $enresult")
                if (enresult.handshakeStatus != SSLEngineResult.HandshakeStatus.NOT_HANDSHAKING) {
                    handshake = true
                    localData = ByteBuffer.allocate(sslEngine.session.applicationBufferSize + 100)
                    manageHandshake()
                }
                println("sslWrite4: ${sslEngine.handshakeStatus}")
            } while (handshake)

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
        try {
            sslEngine.closeInbound()
            socket.close()
        } catch (e: Exception) {
            println("receiveClose.exception: ${e.message}")
            throw e
        }
    }

    private suspend fun sslWrap(writeData: Boolean = true) : SSLEngineResult {
        var result: SSLEngineResult

        try {
            do {
                resetBufForRead(localData)
                resetBufForWrite(networkData)

                println("sslWrap1.localData: $localData, networkData: $networkData")
                println("sslWrap1a: ${sslEngine.handshakeStatus}")
                wrapMutex.lock()
                result = sslEngine.wrap(localData, networkData)
                wrapMutex.unlock()
                println("sslWrap2.result: ${result}, -- localData: $localData, networkData: $networkData")
                engineResultStatus(result.status, true)
                if (result.status == SSLEngineResult.Status.OK) {
                    println("sslWrap3.localData: $localData, networkData: $networkData")
                    if (writeData) {
                        val ret: Int = networkWrite(networkData)
                        if (ret < 0) {
                            println("sslWrap.write failure")
                        }
                        println("sslWrap4.ret: $ret, localData: $localData, networkData: $networkData")
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

    private suspend fun sslUnwrap(readData: Boolean) : SSLEngineResult {
        var result: SSLEngineResult
        val ret: Int
        try {
            println("sslUnwrap1.readData: $readData, localData: $localData, networkData: $networkData")
            if (readData) {
                ret = networkRead(networkData)
                if (ret < 0)
                    println("sslUnwrap read failure")
                println("sslUnwrap3.ret: $ret, localData: $localData, networkData: $networkData")
            } else {
                resetBufForRead(networkData)
            }

            resetBufForWrite(localData)

            do {
                resetBufForRead(networkData)
                println("sslUnwrap4.localData: $localData; networkData: $networkData")
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
            println("sslUnwrap5.localData: $localData; networkData: $networkData")
            return result
        } catch (e: Exception) {
            if (unWrapMutex.isLocked)
                unWrapMutex.unlock()
            println("sslUnwrap.exception: ${e.message}")
            throw e
        }
    }

    private suspend fun sslRunable () {
        var delTask: Runnable? = sslEngine.delegatedTask
        println("sslRunnable")
        while (delTask != null) {
            Thread(delTask).start()
            delTask = sslEngine.delegatedTask
        }
    }

    private suspend fun manageHandshake() {
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

    private suspend fun engineResultStatus(status: SSLEngineResult.Status, wrap: Boolean) {
        println("engineResultStatus: $status, $wrap")
        when (status) {
            SSLEngineResult.Status.BUFFER_OVERFLOW -> {
                //not enough capacity in dest ByteBuffer
                if (wrap)
                    networkData = bufferOverflowMemory(networkData)
                else
                    localData = bufferOverflowMemory(localData)
                println("engineResultStatus.OverFlow.localData: $localData, networkData: $networkData")
                return
            }
            SSLEngineResult.Status.BUFFER_UNDERFLOW -> {
                // not enough capacity in the src ByteBuffer
                if (wrap)
                    localData = bufferUnderflowMemory(localData)
                else
                    networkData = bufferUnderflowMemory(networkData)
                println("engineResultStatus.UnderFlow.localData: $localData, networkData: $networkData")
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

    private suspend fun bufferOverflowMemory(buf: ByteBuffer) : ByteBuffer {
        val appSize:Int = sslEngine.session.applicationBufferSize
        val availSpace: Int = if (buf.limit() == buf.capacity())  buf.limit() - buf.position() else buf.capacity() - buf.limit()
        val tBuf: ByteBuffer = ByteBuffer.allocate(appSize + availSpace + 100)
        resetBufForRead(buf)
        if ((buf.position() == 0) && (buf.limit() == buf.capacity()))
            // buf is empty
            return tBuf
        tBuf.put(buf)
        return tBuf
    }

    private suspend fun bufferUnderflowMemory(buf: ByteBuffer) : ByteBuffer {
        val packetSize: Int = sslEngine.session.packetBufferSize
        val availSpace: Int = if (buf.limit() == buf.capacity())  buf.limit() - buf.position() else buf.capacity() - buf.limit()
        println("bufferUnderflowMemory1.packetSize: $packetSize, availableSpace: $availSpace, capacity: ${buf.capacity()}")
        if (packetSize > availSpace) {
            val tBuf:ByteBuffer = ByteBuffer.allocate(packetSize + availSpace + 100)
            resetBufForRead(buf)
            if ((buf.position() == 0) && (buf.limit() == buf.capacity()))
            // buf is empty
                return tBuf
            tBuf.put(buf)
            return tBuf
        } else {
            //this should not happen for wrap status; happen only for unwrap status
            if (networkRead() < 0)
                println("bufferUnderflowMemory2.read failure")
            return buf
        }
    }

    private suspend fun resetBufForRead(buf: ByteBuffer) {
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

    private suspend fun resetBufForWrite(buf: ByteBuffer) {
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
    private suspend fun networkRead() : Int {
        try {
            //resetBufForWrite(buf)
 //           val jBuf: JvmBuffer = JvmBuffer(buf)
 //           println("networkRead1.jvmBuf: $jBuf")
//            val ret: Int = socket.read(jBuf, timeout)
 //           println("networkRead2.read: $ret, jvmBuf: $jBuf")
            println("networkRead.1: $networkData")
            //val responseData: SocketDataRead<ByteBuffer> = socket.read(timeout, { _: PlatformBuffer, _ -> Unit })
            resetBufForWrite(networkData)
            val responseData: SocketDataRead<ByteBuffer> = socket.read(timeout, {platformBuffer: PlatformBuffer, byteRead: Int ->
                                                                {   if (networkData.remaining() > byteRead)
                                                                        networkData.put((platformBuffer as JvmBuffer).byteBuffer)
                                                                    else
                                                                        println("networkRead.2 need more space")
                                                                }})
            println("networkRead.3: $networkData")
            return responseData.bytesRead
 //           return ret
        } catch (e: Exception) {
            println("networkRead.exception: ${e.message}")
            throw e
        }
    }

    private suspend fun networkWrite() : Int {
        var count: Int = 0
        var sendCount: Int = 0
        try {
           //resetBufForWrite(networkData)
            resetBufForRead(networkData)
            println("networkWrite1: $networkData")
            while (networkData.hasRemaining()) {
                val r = JvmBuffer(networkData)
                println("networkWrite.jvmBuffer: $r")
                val ret:Int = socket.write(r, timeout)
                println("networkWrite2.ret: $ret, networkData: $networkData")
                if (ret < 0) {
                    println("networkWrite3: socket error")
                    return -1
                } else if (ret == 0) {
                    count++
                    if (count > 10) {
                        println("networkWrite4. unable to send data")
                        return -2
                    }
                    sendCount += ret
                }
            }
            return sendCount
        } catch (e: Exception) {
            println("neworkWrite.exception: ${e.message}")
            throw e
        }
    }
}