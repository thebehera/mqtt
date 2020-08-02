
package mqtt.socket

import kotlinx.coroutines.channels.Channel
import mqtt.buffer.BufferMemoryLimit
import mqtt.buffer.BufferPool
import mqtt.buffer.JsBuffer
import mqtt.buffer.PlatformBuffer
import org.khronos.webgl.Uint8Array
import kotlin.time.Duration

open class NodeSocket : ClientSocket {
    var netSocket: Socket? = null
    val pool = BufferPool(limits = object : BufferMemoryLimit {
        override fun isTooLargeForMemory(size: UInt) = false
    })
    internal val incomingMessageChannel = Channel<SocketDataRead<JsBuffer>>(1)

    override fun isOpen() = netSocket?.remoteAddress != null

    override fun localPort() = netSocket?.localPort?.toUShort()

    override fun remotePort() = netSocket?.remotePort?.toUShort()

    override suspend fun <T> read(timeout: Duration, bufferRead: (PlatformBuffer, Int) -> T): SocketDataRead<T> {
        val receivedData = incomingMessageChannel.receive()
        netSocket?.resume()
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
        incomingMessageChannel.close()
        val socket = netSocket
        netSocket = null
        socket?.close()
    }
}

class NodeClientSocket : NodeSocket(), ClientToServerSocket {

    override suspend fun open(
        port: UShort,
        timeout: Duration,
        hostname: String?,
        socketOptions: SocketOptions?
    ): SocketOptions {
        val arrayPlatformBufferMap = HashMap<Uint8Array, JsBuffer>()
        val onRead = OnRead({
            val buffer = pool.borrowAsync() as JsBuffer
            arrayPlatformBufferMap[buffer.buffer] = buffer
            buffer.buffer
        }, { bytesRead, buffer ->
            incomingMessageChannel.offer(SocketDataRead(arrayPlatformBufferMap.remove(buffer)!!, bytesRead))
            false
        })
        val options = tcpOptions(port.toInt(), hostname, onRead)
        val netSocket = connect(options)
        this.netSocket = netSocket
        netSocket.on("error") { err ->
            error(err.toString())
        }
        return SocketOptions()
    }
}
