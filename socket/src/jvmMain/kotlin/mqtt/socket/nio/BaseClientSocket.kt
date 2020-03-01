package mqtt.socket.nio

import mqtt.socket.JvmBuffer
import mqtt.socket.PlatformBuffer
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