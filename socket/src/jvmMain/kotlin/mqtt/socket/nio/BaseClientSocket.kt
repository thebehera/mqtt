package mqtt.socket.nio

import mqtt.buffer.BufferPool
import mqtt.buffer.JvmBuffer
import mqtt.buffer.PlatformBuffer
import mqtt.socket.SocketDataRead
import mqtt.socket.nio.util.aClose
import mqtt.socket.nio.util.read
import mqtt.socket.nio.util.write
import java.net.InetSocketAddress
import java.nio.channels.Selector
import java.nio.channels.SocketChannel
import kotlin.time.Duration
import kotlin.time.ExperimentalTime

@ExperimentalUnsignedTypes
@ExperimentalTime
abstract class BaseClientSocket(
    protected val blocking: Boolean = false,
    override val pool: BufferPool
) : ByteBufferClientSocket<SocketChannel>() {
    val selector = if (!blocking) Selector.open()!! else null

    override fun remotePort() = (socket?.remoteAddress as? InetSocketAddress)?.port?.toUShort()

    override suspend fun read(buffer: PlatformBuffer, timeout: Duration) =
        socket!!.read((buffer as JvmBuffer).byteBuffer, selector, timeout)

    override suspend fun <T> read(
        timeout: Duration,
        bufferRead: suspend (PlatformBuffer, Int) -> T
    ): SocketDataRead<T> {
        var bytesRead = 0
        val result = pool.borrowSuspend {
            val byteBuffer = (it as JvmBuffer).byteBuffer
            bytesRead = socket!!.read(byteBuffer, selector, timeout)
            bufferRead(it, bytesRead)
        }
        return SocketDataRead(result, bytesRead)
    }

    override suspend fun write(buffer: PlatformBuffer, timeout: Duration) =
        socket!!.write((buffer as JvmBuffer).byteBuffer, selector, timeout)

    override suspend fun close() {
        selector?.aClose()
        super.close()
    }

}