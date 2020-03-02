package mqtt.socket.nio2

import mqtt.buffer.JvmBuffer
import mqtt.buffer.PlatformBuffer
import mqtt.socket.nio.ByteBufferClientSocket
import mqtt.socket.nio2.util.aRead
import mqtt.socket.nio2.util.aWrite
import mqtt.socket.nio2.util.assignedPort
import java.nio.channels.AsynchronousSocketChannel
import kotlin.time.Duration
import kotlin.time.ExperimentalTime

@ExperimentalUnsignedTypes
@ExperimentalTime
abstract class AsyncBaseClientSocket : ByteBufferClientSocket<AsynchronousSocketChannel>() {
    override fun remotePort() = socket?.assignedPort(remote = true)

    override suspend fun read(buffer: PlatformBuffer, timeout: Duration) =
        socket!!.aRead((buffer as JvmBuffer).byteBuffer, timeout)

    override suspend fun write(buffer: PlatformBuffer, timeout: Duration) =
        socket!!.aWrite((buffer as JvmBuffer).byteBuffer, timeout)
}