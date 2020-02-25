package mqtt.transport.nio.socket

import kotlinx.coroutines.*
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.consumeAsFlow
import kotlinx.coroutines.flow.flow
import mqtt.transport.BufferPool
import mqtt.transport.ClientSocket
import mqtt.transport.IncomingMessage
import mqtt.transport.PlatformBuffer
import mqtt.transport.nio.JvmBuffer
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
    override val pool: BufferPool,
    override var readTimeout: Duration,
    override var writeTimeout: Duration
) : ClientSocket {

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
                val buffer = pool.borrow() as JvmBuffer
                val bytesRead = aRead(buffer.byteBuffer)
                val byte1 = buffer.readByte()
                val remainingLength = buffer.readVariableByteInteger()
                pool.recycle(buffer)
                val remainingBuffer = pool.borrow(remainingLength)
                aRead((remainingBuffer as JvmBuffer).byteBuffer)
                emit(IncomingMessage(bytesRead, byte1, remainingLength, remainingBuffer))
            }
        } finally {
            close()
        }
    }

    override fun isOpen() = try {
        (socket?.isOpen ?: false) && !isClosing.get() && scope.isActive
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

    override suspend fun send(buffer: PlatformBuffer) = writeChannel.send((buffer as JvmBuffer).byteBuffer)


    override suspend fun close() {
        isClosing.set(true)
        writeChannel.close()
        println("socket close $socket")
        socket?.aClose()
        println("null socket")
        socket = null
    }
}
