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
    private val channel = Channel<PlatformBuffer>(3)
    var lastMessageReceived: TimeMark? = null
    var currentBuffer: ReadBuffer? = null
    var transformer: ((UInt, Byte) -> Byte)? = null

    val readJob = sessionScope.launch {
        var exception: Exception? = null
        try {
            while (isActive && socket.isOpen()) {
                try {
                    val platformBuffer = allocateNewBuffer(8192u)
                    val bytesRead = socket.read(platformBuffer, timeout)
                    lastMessageReceived = TimeSource.Monotonic.markNow()
                    if (bytesRead == -1) {
                        println("done reading")
                        return@launch
                    }
                    channel.send(platformBuffer)
                } catch (e: Exception) {
                    // ignore streaming errors

                    println("read closed ${e.message}")
                    e.printStackTrace()
                    exception = e
                    return@launch
                }
            }
        } finally {
            println("closed: $isActive ${socket.isOpen()} $exception")
            channel.close(exception)
        }
    }


    suspend fun read(bufferSize: Long): ReadBuffer {
        return readBufferWithSize(bufferSize)
    }

    suspend fun <T> readTyped(size: Long, callback: (ReadBuffer) -> T): T {
        val checkBufferResult = readBufferWithSize(size)
        val result = callback(checkBufferResult)
        val buffers = mutableListOf<PlatformBuffer>()

        if (result is FragmentedReadBuffer) {
            result.getBuffers(buffers)
        } else if (result is PlatformBuffer) {
            buffers += result
        }
        return result
    }

    suspend fun readUnsignedByte() = readBufferWithSize(1L).readUnsignedByte()
    suspend fun readByte() = readBufferWithSize(1L).readByte()
    suspend fun readByteArray(size: Long) = readBufferWithSize(size).readByteArray(size.toUInt())


    suspend fun readBufferWithSize(size: Long): ReadBuffer {
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

            val transformer = transformer
            val buffer = if (transformer != null) {
                TransformedReadBuffer(buffers.toComposableBuffer(), transformer)
            } else {
                buffers.toComposableBuffer()
            }
            this.currentBuffer = buffer
            buffer
        } else {
            val transformer = transformer
            val buffer = if (transformer != null) {
                TransformedReadBuffer(currentBuffer, transformer)
            } else {
                currentBuffer
            }
            this.currentBuffer = buffer
            buffer
        }
    }

    suspend fun close() {
        readJob.cancelAndJoin()
    }
}