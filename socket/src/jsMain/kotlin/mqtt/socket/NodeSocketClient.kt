
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

class NodeClientSocket : ClientToServerSocket {
    var netSocket: Socket? = null
    val pool = BufferPool(limits = object : BufferMemoryLimit {
        override fun isTooLargeForMemory(size: UInt) = false
    })
    val incomingMessageChannel = Channel<SocketDataRead<JsBuffer>>()

    override suspend fun open(
        timeout: Duration,
        port: UShort,
        hostname: String?,
        socketOptions: SocketOptions?
    ): SocketOptions {
        val ctx = CoroutineScope(coroutineContext)
        val arrayPlatformBufferMap = HashMap<Uint8Array, JsBuffer>()
        val onRead = OnRead({
            val buffer = pool.borrowAsync() as JsBuffer
            arrayPlatformBufferMap[buffer.buffer] = buffer
            buffer.buffer
        }, { bytesRead, buffer ->
            netSocket?.pause()
            ctx.launch {
                val jsBuffer = arrayPlatformBufferMap[buffer]!!
                incomingMessageChannel.send(SocketDataRead(jsBuffer, bytesRead))
                netSocket?.resume()
            }
            true
        })
        console.log(tcpOptions(port.toInt(), hostname, onRead))
        netSocket = connect(tcpOptions(port.toInt(), hostname, onRead))
        console.log("$netSocket local: ${netSocket?.localPort} ${netSocket?.remoteAddress}:${netSocket?.remotePort}")
        return SocketOptions()
    }

    override fun isOpen() = netSocket?.remoteAddress != null

    override fun localPort() = netSocket?.localPort?.toUShort()

    override fun remotePort() = netSocket?.remotePort?.toUShort()

    override suspend fun <T> read(timeout: Duration, bufferRead: (PlatformBuffer, Int) -> T): SocketDataRead<T> {
        val receivedData = incomingMessageChannel.receive()
        try {
            return SocketDataRead(bufferRead(receivedData.result, receivedData.bytesRead), receivedData.bytesRead)
        } finally {
            pool.recycleAsync(receivedData.result)
        }
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
