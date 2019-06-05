package mqtt.client.transport

import io.ktor.client.features.websocket.DefaultClientWebSocketSession
import io.ktor.http.cio.websocket.Frame
import io.ktor.http.cio.websocket.FrameType
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.ClosedReceiveChannelException
import kotlinx.coroutines.launch
import kotlinx.io.core.*
import mqtt.client.readFirstTwoBytes
import mqtt.time.currentTimestampMs
import mqtt.wire.control.packet.ControlPacket
import mqtt.wire4.control.packet.ControlPacketV4
import kotlin.coroutines.CoroutineContext

class WebSocketTransport(private val websocket: DefaultClientWebSocketSession,
                         override val coroutineContext: CoroutineContext) : Transport, CoroutineScope {
    var lastMessageReceived: Long? = null
    private val buffer = IoBuffer.Pool.borrow()
    private val internalControlPacketChannel = Channel<ControlPacket>()

    init {
        readMessages()
    }

    private fun readMessages() = launch {
        for (frame in websocket.incoming) {
            val packet = readFrames(frame) ?: continue
            lastMessageReceived = currentTimestampMs()
            internalControlPacketChannel.send(packet)
        }
    }

    override suspend fun read(): ControlPacket {
        return internalControlPacketChannel.receive()
    }

    private fun readFrames(frame: Frame): ControlPacket? {
        buffer.writeFully(frame.data)
        val pair = buffer.makeView().readFirstTwoBytes()
        if (pair == null) {
            println("failed to read packet")
            return null
        }
        val (byte1, remaining) = buffer.readFirstTwoBytes()!!
        if (buffer.readRemaining < remaining) {
            return null
        }
        return ControlPacketV4.from(buildPacket { writeFully(buffer, remaining) }, byte1)
    }

    private suspend fun hardClose() {
        dispose()
        awaitClosed()
        throw ClosedReceiveChannelException("Server sent websocket close")
    }

    override suspend fun writePacket(packet: ByteReadPacket) {
        val data = ByteArray(packet.remaining.toInt())
        packet.readFully(data)
        websocket.outgoing.send(Frame.byType(true, FrameType.BINARY, data))
        websocket.flush()
    }

    override fun dispose() {
        buffer.release(IoBuffer.Pool)
        internalControlPacketChannel.close()
        websocket.terminate()
    }

    override suspend fun awaitClosed() {
        websocket.closeReason.await()
    }

    override val isClosed: Boolean = websocket.closeReason.isCompleted
    override val isWebSocket = true
}