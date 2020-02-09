package mqtt.transport.nio2.socket

import kotlinx.coroutines.CoroutineScope
import mqtt.transport.BufferPool
import mqtt.transport.nio.socket.ByteBufferClientSocket
import mqtt.transport.nio2.util.aRead
import mqtt.transport.nio2.util.aWrite
import mqtt.transport.nio2.util.assignedPort
import java.nio.ByteBuffer
import java.nio.channels.AsynchronousSocketChannel
import kotlin.time.Duration
import kotlin.time.ExperimentalTime

@ExperimentalUnsignedTypes
@ExperimentalTime
abstract class AsyncBaseClientSocket(
    coroutineScope: CoroutineScope,
    override val pool: BufferPool<ByteBuffer>,
    override var readTimeout: Duration,
    override var writeTimeout: Duration
) : ByteBufferClientSocket<AsynchronousSocketChannel>(coroutineScope, pool, readTimeout, writeTimeout) {
    override suspend fun aWrite(buffer: ByteBuffer): Int = socket!!.aWrite(buffer, writeTimeout)
    override suspend fun aRead(buffer: ByteBuffer) = socket!!.aRead(buffer, readTimeout)
    override fun remotePort() = socket?.assignedPort(remote = true)

}