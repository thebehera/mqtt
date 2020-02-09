package mqtt.transport.nio2.socket

import kotlinx.coroutines.*
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.consumeAsFlow
import kotlinx.coroutines.flow.flow
import mqtt.transport.BufferPool
import mqtt.transport.ClientSocket
import mqtt.transport.nio2.util.aClose
import mqtt.transport.nio2.util.aRead
import mqtt.transport.nio2.util.aWrite
import mqtt.transport.nio2.util.assignedPort
import java.nio.ByteBuffer
import java.nio.channels.AsynchronousSocketChannel
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.time.Duration
import kotlin.time.ExperimentalTime

@ExperimentalUnsignedTypes
@ExperimentalTime
abstract class AsyncBaseClientSocket(
    coroutineScope: CoroutineScope,
    override val pool: BufferPool<ByteBuffer>,
    override var readTimeout: Duration,
    override var writeTimeout: Duration
) : ClientSocket<ByteBuffer> {
    override val scope: CoroutineScope = coroutineScope + Job()
    protected var socket: AsynchronousSocketChannel? = null
    private val writeChannel = Channel<ByteBuffer>()
    private val isClosing = AtomicBoolean(false)

    override fun isOpen() = try {
        (socket?.isOpen ?: false) && !isClosing.get()
    } catch (e: Throwable) {
        false
    }

    override val incoming = flow {
        try {
            while (isOpen()) {
                val buffer = pool.borrow()
                socket!!.aRead(buffer, readTimeout)
                emit(buffer)
            }
        } finally {
            close()
        }
    }

    protected fun startWriteChannel(socket: AsynchronousSocketChannel) {
        scope.launch {
            try {
                writeChannel.consumeAsFlow().collect {
                    if (isActive) {
                        socket.aWrite(it, writeTimeout)
                    }
                }
            } finally {
                close()
            }
        }
    }


    override fun localPort(): UShort? {
        return socket?.assignedPort(remote = false)
    }

    override fun remotePort(): UShort? {
        return socket?.assignedPort(remote = true)
    }

    override suspend fun send(buffer: ByteBuffer) {
        writeChannel.send(buffer)
    }

    override suspend fun close() {
        writeChannel.close()
        println("base close")
        socket?.aClose()
        socket = null
    }
}