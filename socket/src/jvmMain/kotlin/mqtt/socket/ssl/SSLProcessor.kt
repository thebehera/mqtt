package mqtt.socket.ssl

import kotlinx.coroutines.sync.Mutex
import mqtt.buffer.BufferPool
import mqtt.buffer.JvmBuffer
import mqtt.buffer.PlatformBuffer
import mqtt.socket.nio2.util.aRead
import mqtt.socket.nio2.util.aWrite
import java.nio.ByteBuffer
import java.nio.channels.AsynchronousSocketChannel
import javax.net.ssl.SSLEngine
import javax.net.ssl.SSLEngineResult
import kotlin.time.Duration
import kotlin.time.milliseconds

class SSLProcessor (private val pool: BufferPool, private val sslEngine: SSLEngine, private val socket: AsynchronousSocketChannel) {
    private var timeout: Duration = 5000.milliseconds
    private var networkData: ByteBuffer? = null
    private var localData: ByteBuffer? = null
    private val wrapMutex: Mutex = Mutex()
    private val unWrapMutex: Mutex = Mutex()

/*    init {
        println("SSLProcessor.init.peerHost: ${sslEngine.peerHost}, peerPort: ${sslEngine.peerPort}")
    }
*/
    suspend fun doHandshake() {
//        println("SSLProcessor.doHandshake.begin")
        try {
            pool.borrowSuspend((sslEngine.session.applicationBufferSize.toFloat() * 1.5).toUInt()) { localBuffer ->
                localData = (localBuffer as JvmBuffer).byteBuffer
                pool.borrowSuspend((sslEngine.session.packetBufferSize.toFloat() * 1.5).toUInt()) { netBuffer ->
                    networkData = (netBuffer as JvmBuffer).byteBuffer

//                    println("SSLProcessor.doHandshake: start handshake")
                    sslEngine.beginHandshake()
                    manageHandshake()
                }
                networkData = null
            }
            localData = null
        } catch (e: Exception) {
//            println("doHandshake.error condition")
            throw e
        }
//        println("SSLProcessor.doHandshake: end handshake")
    }

   suspend fun sslRead(timeout: Duration, bufferRead: (PlatformBuffer, Int) -> Unit) {
        this.timeout = timeout
        try {
            pool.borrowSuspend((sslEngine.session.applicationBufferSize.toFloat() * 1.5).toUInt()) { platformBuffer ->
                localData = (platformBuffer as JvmBuffer).byteBuffer
                pool.borrowSuspend((sslEngine.session.packetBufferSize.toFloat() * 1.5).toUInt()) { netBuffer ->
                    networkData = (netBuffer as JvmBuffer).byteBuffer

                    val engineResult: SSLEngineResult = sslUnwrap(true)

//                    println("sslRead1: $engineResult")
                    if (engineResult.status == SSLEngineResult.Status.CLOSED || sslEngine.isInboundDone) {
//                        println("SSL closed by server")
                        socket.close()
                        bufferRead(platformBuffer, -1)
                    }
                    resetBufForRead(localData!!)
/*                    val bytes: ByteArray = ByteArray(localData!!.remaining())
                    localData!!.get(bytes)
                    val x: String = String(bytes, Charset.forName("UTF-8"))
                    localData!!.position(0)
                    println("sslRead2: $x, $platformBuffer, $localData, ${localData!!.remaining()}")

 */                   bufferRead(platformBuffer, localData!!.remaining())
                    //                   println("sslRead3. done")
                }
                networkData = null
            }
            localData = null
        } catch (e: Exception) {
//            println("sslRead4.exception: ${e.message}")
            throw e
        }
    }

    suspend fun sslWrite(buffer: PlatformBuffer) : Int {
        try {
            var remaining: Int
            var writtenLen = 0

            localData = (buffer as JvmBuffer).byteBuffer
            resetBufForRead(localData!!)
//            println("sslWrite1: ${sslEngine.handshakeStatus}, $localData")
            remaining = localData!!.remaining()

            pool.borrowSuspend((sslEngine.session.packetBufferSize.toFloat() * 1.5).toUInt()) { netBuffer ->
                networkData = (netBuffer as JvmBuffer).byteBuffer

                do {
                    val engineResult = sslWrap(true)
                    //                   println("sslWrite2: $engineResult")
                    if (engineResult.handshakeStatus != SSLEngineResult.HandshakeStatus.NOT_HANDSHAKING) {
                        pool.borrowSuspend(
                            (sslEngine.session.applicationBufferSize.toFloat() * 1.5).toUInt(),
                            { localBuffer ->
                                localData = (localBuffer as JvmBuffer).byteBuffer

                                manageHandshake()
                            })
                        localData = buffer.byteBuffer
                    }
                    writtenLen += (remaining - localData!!.remaining())
//                    println("sslWrite3: ${sslEngine.handshakeStatus}, writeLen: $writtenLen")
                } while (localData!!.hasRemaining())
            }
            networkData = null
            localData = null
            return writtenLen
        } catch (e: Exception) {
            throw e
        }
    }

    suspend fun initiateClose() {
        var status:SSLEngineResult.Status = SSLEngineResult.Status.OK

        try {
            pool.borrowSuspend((sslEngine.session.applicationBufferSize.toFloat() * 1.5).toUInt()) { localBuf ->
                localData = (localBuf as JvmBuffer).byteBuffer
                pool.borrowSuspend((sslEngine.session.packetBufferSize.toFloat() * 1.5).toUInt()) { netBuf ->
                    networkData = (netBuf as JvmBuffer).byteBuffer

                    sslEngine.closeOutbound()
                    while (!((status == SSLEngineResult.Status.CLOSED) || sslEngine.isOutboundDone)) {
                        val res: SSLEngineResult = sslWrap()
                        status = res.status
                    }
                    socket.close()
                }
                networkData = null
            }
            localData = null
        } catch (e: Exception) {
            throw e
        }
    }

    suspend fun receivedClose()  {
        try {
            pool.borrowSuspend((sslEngine.session.applicationBufferSize.toFloat() * 1.5).toUInt()) { localBuf ->
                localData = (localBuf as JvmBuffer).byteBuffer
                pool.borrowSuspend((sslEngine.session.packetBufferSize.toFloat() * 1.5).toUInt()) { netBuf ->
                    networkData = (netBuf as JvmBuffer).byteBuffer

                    sslEngine.closeInbound()
                    socket.close()
                }
                networkData = null
            }
            localData = null
        } catch (e: Exception) {
            throw e
        }
    }

    private suspend fun sslWrap(writeData: Boolean = true) : SSLEngineResult {
        var result: SSLEngineResult

        try {
            do {
                resetBufForRead(localData!!)
                resetBufForWrite(networkData!!)

//                println("sslWrap1: ${sslEngine.handshakeStatus}, localData: $localData, networkData: $networkData")
                wrapMutex.lock()
                result = sslEngine.wrap(localData, networkData)
                wrapMutex.unlock()
//                println("sslWrap2.result: ${result}, -- localData: $localData, networkData: $networkData")
                engineResultStatus(result.status, true)
                if (result.status == SSLEngineResult.Status.OK || result.status == SSLEngineResult.Status.CLOSED) {
//                    println("sslWrap3.localData: $localData, networkData: $networkData")
                    if (writeData) {
                        val ret: Int = networkWrite()
                        if (ret < 0) {
                            println("sslWrap4.write failure")
                        }
//                        println("sslWrap5.ret: $ret, localData: $localData, networkData: $networkData")
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
//            println("sslWrap6.exception: ${e.message}")
            throw e
        }
        return result
    }

    private suspend fun sslUnwrap(readData: Boolean) : SSLEngineResult {
        var result: SSLEngineResult

        try {
//            println("sslUnwrap1.readData: $readData, localData: $localData, networkData: $networkData")
            if (readData) {
                if (networkRead() < 0) {
//                    println("sslUnwrap2 read failure")
                    socket.close()
                    throw(Exception("Read failure"))
                }
//                 println("sslUnwrap3.ret: $ret, localData: $localData, networkData: $networkData")
            } else {
                resetBufForRead(networkData!!)
            }

            resetBufForWrite(localData!!)

            do {
                resetBufForRead(networkData!!)
//                println("sslUnwrap4.localData: $localData; networkData: $networkData")
                unWrapMutex.lock()
                result = sslEngine.unwrap(networkData, localData)
                unWrapMutex.unlock()
                engineResultStatus(result.status, false)
//                println("sslUnwrap5.result: $result, localData: $localData, networkData: $networkData")
                if (result.handshakeStatus == SSLEngineResult.HandshakeStatus.NEED_TASK)
                    sslRunable()
            } while ((result.handshakeStatus == SSLEngineResult.HandshakeStatus.NEED_UNWRAP) ||
                (result.handshakeStatus == SSLEngineResult.HandshakeStatus.NEED_TASK) ||
                (result.status == SSLEngineResult.Status.BUFFER_OVERFLOW) ||
                (result.status == SSLEngineResult.Status.BUFFER_UNDERFLOW))
//            println("sslUnwrap6.localData: $localData; networkData: $networkData")
            return result
        } catch (e: Exception) {
            if (unWrapMutex.isLocked)
                unWrapMutex.unlock()
//            println("sslUnwrap7.exception: ${e.message}")
            throw e
        }
    }

    private fun sslRunable () {
        var delTask: Runnable? = sslEngine.delegatedTask
//        println("sslRunnable")
        while (delTask != null) {
            Thread(delTask).start()
            delTask = sslEngine.delegatedTask
        }
    }

    private suspend fun manageHandshake() {
        var hstatus : SSLEngineResult.HandshakeStatus = sslEngine.handshakeStatus

        try {
            while (true) {
//                println("manageHandshake1: $hstatus")
                when (hstatus) {
                    SSLEngineResult.HandshakeStatus.FINISHED -> {
                        return
                    }
                    SSLEngineResult.HandshakeStatus.NEED_TASK -> {
                        //    sslRunable()
//                        println("manageHandshake2. *******should never reach here*******")
                    }
                    SSLEngineResult.HandshakeStatus.NEED_UNWRAP -> {
                        val result: SSLEngineResult = sslUnwrap(true)

                        hstatus = result.handshakeStatus
                    }
                    SSLEngineResult.HandshakeStatus.NEED_WRAP -> {
                        val result: SSLEngineResult = sslWrap(true)

                        hstatus = result.handshakeStatus
                    }
                    SSLEngineResult.HandshakeStatus.NOT_HANDSHAKING -> {
//                        println("manageHandshake3.not_handshaking")
                        return
                    }
                    SSLEngineResult.HandshakeStatus.NEED_UNWRAP_AGAIN -> {
//                        println("manageHandshakeneed4_unwrap_again")
                        return
                    }
                }
            }
        } catch (e: Exception) {
//            println("manageHandshake5.exception: ${e.message}")
            throw e
        }
    }

    private suspend fun engineResultStatus(status: SSLEngineResult.Status, wrap: Boolean) {
//        println("engineResultStatus: $status, $wrap")
        when (status) {
            SSLEngineResult.Status.BUFFER_OVERFLOW -> {
                //not enough capacity in dest ByteBuffer
                if (wrap)
                    networkData = bufferOverflowMemory(networkData!!)
                else
                    localData = bufferOverflowMemory(localData!!)
//                println("engineResultStatus.OverFlow.localData: $localData, networkData: $networkData")
                return
            }
            SSLEngineResult.Status.BUFFER_UNDERFLOW -> {
                // not enough capacity in the src ByteBuffer
                if (wrap)
                    localData = bufferUnderflowMemory(localData!!)
                else
                    networkData = bufferUnderflowMemory(networkData!!)
//                println("engineResultStatus.UnderFlow.localData: $localData, networkData: $networkData")
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

    private fun bufferOverflowMemory(buf: ByteBuffer) : ByteBuffer {
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
//        println("bufferUnderflowMemory1.packetSize: $packetSize, availableSpace: $availSpace, capacity: ${buf.capacity()}")
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

    private fun resetBufForRead(buf: ByteBuffer) {
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

    private fun resetBufForWrite(buf: ByteBuffer) {
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
//            println("networkRead1: $networkData")
            //val responseData: SocketDataRead<ByteBuffer> = socket.read(timeout, { _: PlatformBuffer, _ -> Unit })
            resetBufForWrite(networkData!!)
            val readInt = socket.aRead(networkData!!, timeout)
 /*           val responseData: SocketDataRead<ByteBuffer> = socket.read(timeout, {platformBuffer: PlatformBuffer, byteRead: Int ->
                {   if (networkData.remaining() > byteRead)
                    networkData.put((platformBuffer as JvmBuffer).byteBuffer)
                else
                    println("networkRead.2 need more space")
                }})
  */
 //           println("networkRead2: readLen: $readInt, $networkData")
            return readInt
        } catch (e: Exception) {
//            println("networkRead3.exception: ${e.message}")
            throw e
        }
    }

    private suspend fun networkWrite() : Int {
        var count = 0
        var sendCount = 0
        try {
            //resetBufForWrite(networkData)
            //resetBufForRead(networkData!!)
//            println("networkWrite1: $networkData")
            while (networkData!!.hasRemaining()) {
//                println("networkWrite2.networkData: $networkData")
                val writeLen = socket.aWrite(networkData!!, timeout)

//                println("networkWrite3.ret: $writeLen, networkData: $networkData")
                if (writeLen < 0) {
//                    println("networkWrite4: socket error")
                    return -1
                } else if (writeLen == 0) {
                    count++
                    if (count > 10) {
//                        println("networkWrite5. unable to send data")
                        return -2
                    }
                }
                sendCount += writeLen
            }
            return sendCount
        } catch (e: Exception) {
//            println("neworkWrite6.exception: ${e.message}")
            throw e
        }
    }
}
