package mqtt.transport.nio2.socket

import mqtt.transport.PlatformBuffer
import mqtt.transport.nio.JvmBuffer
import mqtt.transport.nio.socket.ByteBufferClientSocket
import mqtt.transport.nio2.util.aRead
import mqtt.transport.nio2.util.aWrite
import mqtt.transport.nio2.util.assignedPort
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