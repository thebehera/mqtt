package mqtt.socket.nio

import mqtt.socket.ClientSocket
import mqtt.socket.nio.util.aClose
import java.net.InetSocketAddress
import java.nio.channels.NetworkChannel
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.time.ExperimentalTime

@ExperimentalUnsignedTypes
@ExperimentalTime
abstract class ByteBufferClientSocket<T : NetworkChannel> : ClientSocket {
    protected var socket: T? = null
    private val isClosing = AtomicBoolean(false)

    override fun isOpen() = try {
        (socket?.isOpen ?: false) && !isClosing.get()
    } catch (e: Throwable) {
        false
    }

    override fun localPort(): UShort? = (socket?.localAddress as? InetSocketAddress)?.port?.toUShort()

    override suspend fun close() {
        isClosing.set(true)
        socket?.aClose()
        socket = null
    }
}
