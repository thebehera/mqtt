
package mqtt.socket

import mqtt.buffer.allocateNewBuffer
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.ClosedSendChannelException
import mqtt.buffer.BufferMemoryLimit
import mqtt.buffer.JsBuffer
import mqtt.buffer.PlatformBuffer
import org.khronos.webgl.Uint8Array
import kotlin.time.Duration

open class NodeSocket : ClientSocket {
    var netSocket: Socket? = null
    internal val incomingMessageChannel = Channel<SocketDataRead<JsBuffer>>(1)

    override fun isOpen() = netSocket?.remoteAddress != null

    override fun localPort() = netSocket?.localPort?.toUShort()

    override fun remotePort() = netSocket?.remotePort?.toUShort()

    override suspend fun read(buffer: PlatformBuffer, timeout: Duration): Int {
        val receivedData = incomingMessageChannel.receive()
        netSocket?.resume()
        buffer.put(receivedData.result)
        receivedData.result.put(buffer)
        buffer.position(0)
        buffer.setLimit(receivedData.bytesRead)
        return receivedData.bytesRead
    }

    override suspend fun <T> read(
        timeout: Duration,
        bufferRead: suspend (PlatformBuffer, Int) -> T
    ): SocketDataRead<T> {
        val receivedData = incomingMessageChannel.receive()
        netSocket?.resume()
        return SocketDataRead(bufferRead(receivedData.result, receivedData.bytesRead), receivedData.bytesRead)
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
        socket?.destroy()
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
            val buffer = allocateNewBuffer(8192u) as JsBuffer
            arrayPlatformBufferMap[buffer.buffer] = buffer
            buffer.buffer
        }, { bytesRead, buffer ->
            try {
                incomingMessageChannel.offer(SocketDataRead(arrayPlatformBufferMap.remove(buffer)!!, bytesRead))
            } catch (e: ClosedSendChannelException) {
                println("ignore closed send channel excp")
            }
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
