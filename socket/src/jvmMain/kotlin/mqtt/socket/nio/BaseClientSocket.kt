package mqtt.socket.nio

import mqtt.buffer.allocateNewBuffer
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
    protected val blocking: Boolean = false
) : ByteBufferClientSocket<SocketChannel>() {
    val selector = if (!blocking) Selector.open()!! else null

    override fun remotePort() = (socket?.remoteAddress as? InetSocketAddress)?.port?.toUShort()

    override suspend fun read(buffer: PlatformBuffer, timeout: Duration) =
        socket!!.read((buffer as JvmBuffer).byteBuffer, selector, timeout)

    override suspend fun <T> read(
        timeout: Duration,
        bufferRead: suspend (PlatformBuffer, Int) -> T
    ): SocketDataRead<T> {
        val buffer = allocateNewBuffer(8196u)
        val byteBuffer = (buffer as JvmBuffer).byteBuffer
        val bytesRead = socket!!.read(byteBuffer, selector, timeout)
        val result = bufferRead(buffer, bytesRead)
        return SocketDataRead(result, bytesRead)
    }

    override suspend fun write(buffer: PlatformBuffer, timeout: Duration) =
        socket!!.write((buffer as JvmBuffer).byteBuffer, selector, timeout)

    override suspend fun close() {
        selector?.aClose()
        super.close()
    }

}