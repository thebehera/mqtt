package mqtt.client.websocket

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.ClosedSendChannelException
import kotlinx.coroutines.channels.SendChannel
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.consumeAsFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import mqtt.buffer.PlatformBuffer
import mqtt.buffer.allocateNewBuffer
import mqtt.client.loadCustomWebsocketImplementation
import mqtt.client.socket.ISocketController
import mqtt.connection.IConnectionOptions
import mqtt.socket.ClientSocket
import mqtt.socket.SuspendingInputStream
import mqtt.socket.getClientSocket
import mqtt.wire.control.packet.ControlPacket
import mqtt.wire.control.packet.ControlPacketFactory
import kotlin.experimental.and
import kotlin.experimental.or
import kotlin.experimental.xor
import kotlin.random.Random
import kotlin.time.Duration
import kotlin.time.ExperimentalTime
import kotlin.time.TimeMark


@ExperimentalUnsignedTypes
@ExperimentalTime
class WebsocketController private constructor(
    private val scope: CoroutineScope,
    controlPacketFactory: ControlPacketFactory,
    private val socket: ClientSocket,
    keepAliveTimeout: Duration,
    private val writeQueue: SendChannel<Collection<ControlPacket>>,
) : ISocketController {
    override var lastMessageReceived: TimeMark? = null
    private val suspendingInputStream = SuspendingInputStream(keepAliveTimeout, scope, socket)
    private val inputStream =
        WebsocketSuspendableInputStream(suspendingInputStream, controlPacketFactory)

    override suspend fun write(controlPacket: ControlPacket) {
        write(listOf(controlPacket))
    }

    override suspend fun write(controlPackets: Collection<ControlPacket>) {
        try {
            writeQueue.send(controlPackets)
        } catch (e: ClosedSendChannelException) {
            // ignore closed channels
        }
    }

    override suspend fun read() = flow {
        while (scope.isActive) {
            val packet = inputStream.readPacket() ?: return@flow
            lastMessageReceived = suspendingInputStream.lastMessageReceived
            emit(packet)
        }
    }

    override suspend fun close() {
        writeQueue.close()
        socket.close()
    }

    companion object {
        private const val websocketBaseFrameOverhead = 6
        suspend fun openWebSocket(
            scope: CoroutineScope,
            connectionOptions: IConnectionOptions
        ): ISocketController? {
            val websocketEndpoint = connectionOptions.websocketEndpoint
            val socket = getClientSocket()
                ?: (loadCustomWebsocketImplementation(scope, connectionOptions)?.let { return it }
                    ?: throw IllegalStateException("Impossible WS state"))
            socket.open(port = connectionOptions.port.toUShort(), hostname = connectionOptions.name)
            val request =
                "GET $websocketEndpoint HTTP/1.1\r\nHost: ${connectionOptions.name}:${connectionOptions.port}\r\nUpgrade: websocket\r\nConnection: Upgrade\r\nSec-WebSocket-Key: dGhlIHNhbXBsZSBub25jZQ==\r\nSec-WebSocket-Protocol: mqtt\r\nSec-WebSocket-Version: 13\r\n\r\n"
            socket.write(request, connectionOptions.connectionTimeout)
            val response = socket.read().result
            if (!(response.contains("101 Switching Protocols", ignoreCase = true)
                        && response.contains("Upgrade: websocket", ignoreCase = true)
                        && response.contains("Connection: Upgrade", ignoreCase = true)
                        && response.contains("Sec-WebSocket-Accept", ignoreCase = true))
            ) {
                throw IllegalStateException("Invalid response from server when reading the result from websockets. Response:\r\n$response")
            }
            val writeQueue = Channel<Collection<ControlPacket>>(Channel.RENDEZVOUS)
            scope.launch {
                while (isActive && socket.isOpen()) {
                    writeQueue.consumeAsFlow().collect { packets ->
                        val payloadSize = packets.fold(0u) { acc, controlPacket ->
                            acc + controlPacket.packetSize()
                        }
                        val websocketFrameOverhead = when {
                            payloadSize > UShort.MAX_VALUE -> websocketBaseFrameOverhead + 8
                            payloadSize >= 126u -> websocketBaseFrameOverhead + 2
                            else -> websocketBaseFrameOverhead
                        }
                        val length = payloadSize.toLong() + websocketFrameOverhead.toLong()
                        val writeBuffer = allocateNewBuffer(length.toUInt())
                        appendFinAndOpCode(writeBuffer, 2, true)
                        val mask = Random.Default.nextBytes(4)
                        appendLengthAndMask(writeBuffer, payloadSize.toInt(), mask)
                        val startPayloadPosition = writeBuffer.position()
                        // write the serialized data
                        packets.forEach { it.serialize(writeBuffer) }
                        // reset position to original, we are going to mask these values
                        for ((count, position) in (startPayloadPosition.toInt() until length).withIndex()) {
                            writeBuffer.position(position.toInt())
                            val payloadByte = writeBuffer.readByte()
                            val maskValue = mask[count % 4]
                            val maskedByte = payloadByte xor maskValue
                            writeBuffer.position(position.toInt())
                            writeBuffer.write(maskedByte)
                        }
                        socket.write(writeBuffer, connectionOptions.request.keepAliveTimeout)
                    }
                }
            }
            return WebsocketController(
                scope,
                connectionOptions.request.controlPacketFactory,
                socket,
                connectionOptions.request.keepAliveTimeout,
                writeQueue
            )
        }

        private fun appendFinAndOpCode(buffer: PlatformBuffer, opcode: Byte, fin: Boolean) {
            var b: Byte = 0x00
            // Add Fin flag
            if (fin) {
                b = b or 0x80.toByte()
            }
            // RSV 1,2,3 aren't important
            // Add opcode
            b = b or (opcode and 0x0F)
            buffer.write(b)
        }

        private fun appendLengthAndMask(buffer: PlatformBuffer, length: Int, mask: ByteArray) {
            appendLength(buffer, length, true)
            buffer.write(mask)
        }

        private fun appendLength(buffer: PlatformBuffer, length: Int, masked: Boolean) {
            if (length < 0) {
                throw IllegalArgumentException("Length cannot be negative")
            }
            val b = if (masked) 0x80.toByte() else 0x00
            when {
                length > 0xFFFF -> {
                    buffer.write((b or 0x7F))
                    buffer.write(0x00.toByte())
                    buffer.write(0x00.toByte())
                    buffer.write(0x00.toByte())
                    buffer.write(0x00.toByte())
                    buffer.write((length shr 24 and 0xFF).toByte())
                    buffer.write((length shr 16 and 0xFF).toByte())
                    buffer.write((length shr 8 and 0xFF).toByte())
                    buffer.write((length and 0xFF).toByte())
                }
                length >= 0x7E -> {
                    buffer.write((b or 0x7E))
                    buffer.write((length shr 8).toByte())
                    buffer.write((length and 0xFF).toByte())
                }
                else -> {
                    buffer.write((b or length.toByte()))
                }
            }
        }

    }
}
