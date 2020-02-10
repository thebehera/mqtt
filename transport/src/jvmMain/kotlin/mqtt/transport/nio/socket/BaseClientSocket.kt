package mqtt.transport.nio.socket

import kotlinx.coroutines.CoroutineScope
import mqtt.transport.BufferPool
import mqtt.transport.nio.socket.util.aClose
import mqtt.transport.nio.socket.util.read
import mqtt.transport.nio.socket.util.write
import java.net.InetSocketAddress
import java.nio.ByteBuffer
import java.nio.channels.Selector
import java.nio.channels.SocketChannel
import kotlin.time.Duration
import kotlin.time.ExperimentalTime

@ExperimentalUnsignedTypes
@ExperimentalTime
abstract class BaseClientSocket(
    coroutineScope: CoroutineScope,
    override val pool: BufferPool<ByteBuffer>,
    override var readTimeout: Duration,
    override var writeTimeout: Duration
) : ByteBufferClientSocket<SocketChannel>(coroutineScope, pool, readTimeout, writeTimeout) {
    val selector by lazy { Selector.open()!! }

    override suspend fun aWrite(buffer: ByteBuffer) = socket!!.write(scope, buffer, selector, writeTimeout)

    override suspend fun aRead(buffer: ByteBuffer) = socket!!.read(scope, buffer, selector, readTimeout)

    override fun remotePort() = (socket?.remoteAddress as? InetSocketAddress)?.port?.toUShort()

    override suspend fun close() {
        selector.aClose()
        super.close()
    }

}