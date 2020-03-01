package mqtt.transport.nio.socket

import mqtt.transport.PlatformBuffer
import mqtt.transport.nio.JvmBuffer
import mqtt.transport.nio.socket.util.aClose
import mqtt.transport.nio.socket.util.read
import mqtt.transport.nio.socket.util.write
import java.net.InetSocketAddress
import java.nio.channels.Selector
import java.nio.channels.SocketChannel
import kotlin.time.Duration
import kotlin.time.ExperimentalTime

@ExperimentalUnsignedTypes
@ExperimentalTime
abstract class BaseClientSocket(protected val blocking: Boolean = false) : ByteBufferClientSocket<SocketChannel>() {
    val selector = if (!blocking) Selector.open()!! else null

    override fun remotePort() = (socket?.remoteAddress as? InetSocketAddress)?.port?.toUShort()

    override suspend fun read(buffer: PlatformBuffer, timeout: Duration) =
        socket!!.read((buffer as JvmBuffer).byteBuffer, selector, timeout)

    override suspend fun write(buffer: PlatformBuffer, timeout: Duration) =
        socket!!.write((buffer as JvmBuffer).byteBuffer, selector, timeout)

    override suspend fun close() {
        selector?.aClose()
        super.close()
    }

}