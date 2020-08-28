package mqtt.socket.nio2

import mqtt.buffer.BufferPool
import mqtt.buffer.JvmBuffer
import mqtt.buffer.PlatformBuffer
import mqtt.socket.SocketDataRead
import mqtt.socket.nio.ByteBufferClientSocket
import mqtt.socket.nio2.util.aRead
import mqtt.socket.nio2.util.aWrite
import mqtt.socket.nio2.util.assignedPort
import mqtt.socket.ssl.SSLProcessor
import java.nio.channels.AsynchronousSocketChannel
import kotlin.time.Duration
import kotlin.time.ExperimentalTime

@ExperimentalUnsignedTypes
@ExperimentalTime
abstract class AsyncBaseClientSocket(private val pool: BufferPool, private val ssl: Boolean = false) :
    ByteBufferClientSocket<AsynchronousSocketChannel>() {
    override fun remotePort() = socket?.assignedPort(remote = true)
    protected lateinit var sslProcess: SSLProcessor

    override suspend fun <T> read(timeout: Duration, bufferRead: (PlatformBuffer, Int) -> T): SocketDataRead<T> {
        var result: T? = null
        var bytesRead = 0

        if (ssl) {
            sslProcess.sslRead(timeout) { buf: PlatformBuffer, size: Int ->
                bytesRead = size
                result = bufferRead(buf, size)
            }
        } else {
            result = pool.borrowSuspend {
                val byteBuffer = (it as JvmBuffer).byteBuffer
                bytesRead = socket!!.aRead(byteBuffer, timeout)
                bufferRead(it, bytesRead)
            }
        }
        return SocketDataRead(result!!, bytesRead)
    }

    override suspend fun write(buffer: PlatformBuffer, timeout: Duration): Int {
        return if (ssl)
            sslProcess.sslWrite(buffer)
        else
            socket!!.aWrite((buffer as JvmBuffer).byteBuffer, timeout)
    }

    override suspend fun close() {
        if (ssl)
            sslProcess.initiateClose()
        super.close()
    }
}