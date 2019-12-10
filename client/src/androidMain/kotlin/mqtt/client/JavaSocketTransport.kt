package mqtt.client

import io.ktor.network.sockets.*
import kotlinx.io.core.ByteReadPacket
import mqtt.client.transport.Transport

class JavaSocketTransport(private val socket: Socket) : Transport {
    private val writeChannel by lazy { socket.openWriteChannel(autoFlush = true) }
    private val readChannel by lazy { socket.openReadChannel() }
    override suspend fun read() = readChannel.read()
    override suspend fun writePacket(packet: ByteReadPacket) = writeChannel.writePacket(packet)
    override suspend fun awaitClosed() = socket.awaitClosed()
    override val isClosed: Boolean = socket.isClosed
    override fun dispose() = socket.dispose()
    override val isWebSocket: Boolean = false
}
