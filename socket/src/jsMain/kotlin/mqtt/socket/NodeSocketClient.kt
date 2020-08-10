
package mqtt.socket

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.launch
import mqtt.buffer.BufferMemoryLimit
import mqtt.buffer.BufferPool
import mqtt.buffer.JsBuffer
import mqtt.buffer.PlatformBuffer
import org.khronos.webgl.Uint8Array
import kotlin.coroutines.coroutineContext
import kotlin.time.Duration

open class NodeSocket : ClientSocket {
    var netSocket: Socket? = null
    val pool = BufferPool(limits = object : BufferMemoryLimit {
        override fun isTooLargeForMemory(size: UInt) = false
    })
    protected val incomingMessageChannel = Channel<SocketPlatformBufferRead>()

    override fun isOpen() = netSocket?.remoteAddress != null

    override fun localPort() = netSocket?.localPort?.toUShort()

    override fun remotePort() = netSocket?.remotePort?.toUShort()

    override suspend fun <T> read(timeout: Duration, bufferRead: (PlatformBuffer, Int) -> T): SocketDataRead<T> {
        val receivedData = incomingMessageChannel.receive()
        val jsBuffer = receivedData.bufferRead as JsBuffer
        try {
            return SocketDataRead(bufferRead(jsBuffer, receivedData.bytesRead), receivedData.bytesRead)
        } finally {
            receivedData.recycleCallback.recycle()
        }
    }

    override suspend fun read(timeout: Duration): SocketPlatformBufferRead {
        val receivedData = incomingMessageChannel.receive()
        val jsBuffer = receivedData.bufferRead as JsBuffer
        return SocketPlatformBufferRead(jsBuffer, receivedData.bytesRead, receivedData.recycleCallback)
    }

    override suspend fun write(buffer: PlatformBuffer, timeout: Duration): Int {
        val array = (buffer as JsBuffer).buffer
        val netSocket = netSocket ?: return 0
        netSocket.write(array)
        return array.byteLength
    }

    override suspend fun close() {
        val socket = netSocket
        netSocket = null
        socket?.close()
    }
}

class NodeClientSocket : NodeSocket(), ClientToServerSocket {

    override suspend fun open(
        timeout: Duration,
        port: UShort,
        hostname: String?,
        socketOptions: SocketOptions?
    ): SocketOptions {
        val ctx = CoroutineScope(coroutineContext)
        val arrayPlatformBufferMap = HashMap<Uint8Array, Pair<JsBuffer, BufferPool.RecycleCallback>>()
        val onRead = OnRead({
            val (platformBuffer, recycleCallback) = pool.borrowLaterCallRecycleCallback()
            val buffer = platformBuffer as JsBuffer
            arrayPlatformBufferMap[buffer.buffer] = Pair(buffer, recycleCallback)
            buffer.buffer
        }, { bytesRead, buffer ->
            netSocket?.pause()
            ctx.launch {
                val (jsBuffer, recycleCallback) = arrayPlatformBufferMap[buffer]!!
                incomingMessageChannel.send(SocketPlatformBufferRead(jsBuffer, bytesRead, recycleCallback))
                netSocket?.resume()
            }
            true
        })
        console.log(tcpOptions(port.toInt(), hostname, onRead))
        netSocket = connect(tcpOptions(port.toInt(), hostname, onRead))
        console.log("$netSocket local: ${netSocket?.localPort} ${netSocket?.remoteAddress}:${netSocket?.remotePort}")
        return SocketOptions()
    }
}
