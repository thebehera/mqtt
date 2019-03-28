package mqtt.client

import io.ktor.network.sockets.*
import kotlinx.coroutines.io.ByteReadChannel
import kotlinx.coroutines.io.ByteWriteChannel

class JavaPlatformSocket(private val socket: Socket) : PlatformSocket {
    override val output: ByteWriteChannel = socket.openWriteChannel(autoFlush = true)
    override val input: ByteReadChannel = socket.openReadChannel()
    override suspend fun awaitClosed() = socket.awaitClosed()
    override val isClosed: Boolean = socket.isClosed
    override fun dispose() = socket.dispose()
}
