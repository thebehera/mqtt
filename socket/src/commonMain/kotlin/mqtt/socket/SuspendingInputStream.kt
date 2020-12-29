package mqtt.socket

import kotlinx.coroutines.*
import kotlinx.coroutines.channels.Channel
import mqtt.buffer.*
import kotlin.experimental.and
import kotlin.time.Duration
import kotlin.time.ExperimentalTime
import kotlin.time.TimeMark
import kotlin.time.TimeSource

@ExperimentalUnsignedTypes
@ExperimentalTime
class SuspendingInputStream(timeout: Duration, val sessionScope: CoroutineScope, val socket: ClientSocket) {
    private val channel = Channel<PlatformBuffer>()
    lateinit var lastMessageReceived: TimeMark
    var currentBuffer: ReadBuffer? = null

    val readJob = sessionScope.launch {
        var exception: Exception? = null
        try {
            while (isActive && socket.isOpen()) {
                try {
                    val platformBuffer = socket.pool.borrowAsync()
                    val bytesRead = socket.read(platformBuffer, timeout)
                    lastMessageReceived = TimeSource.Monotonic.markNow()
                    if (bytesRead == -1) {
                        return@launch
                    }
                    channel.send(platformBuffer)
                } catch (e: Exception) {
                    // ignore streaming errors
                    exception = e
                    return@launch
                }
            }
        } finally {
            channel.close(exception)
        }
    }

    suspend fun <T> readTyped(size: Long, callback: (ReadBuffer) -> T): T {
        val checkBufferResult = checkBuffers(size)
        val result = callback(checkBufferResult)
        val buffers = mutableListOf<PlatformBuffer>()
        if (result is FragmentedReadBuffer) {
            result.getBuffers(buffers)
        } else if (result is PlatformBuffer) {
            buffers += result
        }
        buffers.filter { !it.hasRemaining() }.forEach { socket.pool.recycleAsync(it) }
        return result
    }

    suspend fun readUnsignedByte() = checkBuffers(1L).readUnsignedByte()
    suspend fun readByte() = checkBuffers(1L).readByte()

    suspend fun readVariableByteInteger(): UInt {
        var digit: Byte
        var value = 0L
        var multiplier = 1L
        var count = 0L
        try {
            do {
                digit = readByte()
                count++
                value += (digit and 0x7F).toLong() * multiplier
                multiplier *= 128
            } while ((digit and 0x80.toByte()).toInt() != 0)
        } catch (e: Exception) {
            throw MalformedInvalidVariableByteInteger(value.toUInt())
        }
        if (value < 0 || value > VARIABLE_BYTE_INT_MAX.toLong()) {
            throw MalformedInvalidVariableByteInteger(value.toUInt())
        }
        return value.toUInt()
    }


    private suspend fun checkBuffers(size: Long): ReadBuffer {
        val bufferTmp = currentBuffer
        val currentBuffer = if (bufferTmp != null && bufferTmp.remaining().toLong() >= size) {
            bufferTmp
        } else {
            channel.receive()
        }
        var extraBufferNeededWithSize = size - currentBuffer.remaining().toLong()
        return if (extraBufferNeededWithSize > 0) {
            // we need to read more data
            val buffers = mutableListOf(currentBuffer)
            while (extraBufferNeededWithSize > 0) {
                val nextBuffer = channel.receive()
                extraBufferNeededWithSize -= nextBuffer.remaining().toLong()
                buffers += nextBuffer
            }

            val buffer = buffers.toComposableBuffer()
            this.currentBuffer = buffer
            buffer
        } else {
            this.currentBuffer = currentBuffer
            return currentBuffer
        }
    }

    suspend fun close() {
        readJob.cancelAndJoin()
    }
}