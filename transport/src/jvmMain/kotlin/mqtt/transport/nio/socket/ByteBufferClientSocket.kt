package mqtt.transport.nio.socket

import kotlinx.coroutines.*
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.consumeAsFlow
import kotlinx.coroutines.flow.flow
import mqtt.transport.BufferPool
import mqtt.transport.ClientSocket
import mqtt.transport.nio.socket.util.aClose
import java.net.InetSocketAddress
import java.nio.ByteBuffer
import java.nio.channels.NetworkChannel
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.time.Duration
import kotlin.time.ExperimentalTime

@ExperimentalUnsignedTypes
@ExperimentalTime
abstract class ByteBufferClientSocket<T : NetworkChannel>(
    coroutineScope: CoroutineScope,
    override val pool: BufferPool<ByteBuffer>,
    override var readTimeout: Duration,
    override var writeTimeout: Duration
) : ClientSocket<ByteBuffer> {

    override val scope: CoroutineScope = coroutineScope + Job()
    private val writeChannel = Channel<ByteBuffer>()
    protected var socket: T? = null
    private val isClosing = AtomicBoolean(false)
    override var tag: Any? = null

    open fun setupIncomingBuffer() {}
    override val incoming = flow {
        try {
            setupIncomingBuffer()
            while (isOpen()) {
                val buffer = pool.borrow()
                aRead(buffer)
                emit(buffer)
            }
        } finally {
            close()
        }
    }

    override fun isOpen() = try {
        (socket?.isOpen ?: false) && !isClosing.get()
    } catch (e: Throwable) {
        false
    }

    abstract suspend fun aRead(buffer: ByteBuffer): Int
    abstract suspend fun aWrite(buffer: ByteBuffer): Int

    protected fun startWriteChannel() = scope.launch {
        try {
            writeChannel.consumeAsFlow().collect {
                if (isActive) {
                    aWrite(it)
                }
            }
        } finally {
            close()
        }
    }


    override fun localPort(): UShort? = (socket?.localAddress as? InetSocketAddress)?.port?.toUShort()

    override suspend fun send(buffer: ByteBuffer) = writeChannel.send(buffer)


    override suspend fun close() {
        isClosing.set(true)
        writeChannel.close()
        socket?.aClose()
        socket = null
    }
}