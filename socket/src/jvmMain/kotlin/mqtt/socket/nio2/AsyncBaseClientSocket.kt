package mqtt.socket.nio2

import mqtt.buffer.BufferPool
import mqtt.buffer.JvmBuffer
import mqtt.buffer.PlatformBuffer
import mqtt.socket.SocketDataRead
import mqtt.socket.SocketPlatformBufferRead
import mqtt.socket.nio.ByteBufferClientSocket
import mqtt.socket.nio2.util.aRead
import mqtt.socket.nio2.util.aWrite
import mqtt.socket.nio2.util.assignedPort
import java.nio.channels.AsynchronousSocketChannel
import kotlin.time.Duration
import kotlin.time.ExperimentalTime

@ExperimentalUnsignedTypes
@ExperimentalTime
abstract class AsyncBaseClientSocket(private val pool: BufferPool) :
    ByteBufferClientSocket<AsynchronousSocketChannel>() {
    override fun remotePort() = socket?.assignedPort(remote = true)

    override suspend fun <T> read(timeout: Duration, bufferRead: (PlatformBuffer, Int) -> T): SocketDataRead<T> {
        var bytesRead = 0
        val result = pool.borrowSuspend {
            val byteBuffer = (it as JvmBuffer).byteBuffer
            bytesRead = socket!!.aRead(byteBuffer, timeout)
            bufferRead(it, bytesRead)
        }
        return SocketDataRead(result, bytesRead)
    }

    override suspend fun read(timeout: Duration): SocketPlatformBufferRead {
        val (buffer, recycleCallback) = pool.borrowLaterCallRecycleCallback()
        val byteBuffer = (buffer as JvmBuffer).byteBuffer
        val bytesRead = socket!!.aRead(byteBuffer, timeout)
        return SocketPlatformBufferRead(buffer, bytesRead, recycleCallback)
    }

    override suspend fun write(buffer: PlatformBuffer, timeout: Duration) =
        socket!!.aWrite((buffer as JvmBuffer).byteBuffer, timeout)
}