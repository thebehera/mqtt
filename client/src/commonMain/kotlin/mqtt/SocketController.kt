package mqtt

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.ClosedSendChannelException
import kotlinx.coroutines.flow.FlowCollector
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.consumeAsFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import mqtt.buffer.PlatformBuffer
import mqtt.socket.ClientSocket
import mqtt.wire.control.packet.ControlPacket
import mqtt.wire.control.packet.ControlPacketFactory
import kotlin.time.Duration
import kotlin.time.ExperimentalTime
import kotlin.time.TimeMark
import kotlin.time.TimeSource

@ExperimentalUnsignedTypes
@ExperimentalTime
class SocketController(
    private val scope: CoroutineScope,
    private val controlPacketFactory: ControlPacketFactory,
    private val socket: ClientSocket,
    private val keepAliveTimeout: Duration
) {
    private val writeQueue = Channel<Collection<ControlPacket>>(Channel.RENDEZVOUS)
    lateinit var lastMessageReceived: TimeMark
    init {
        scope.launch {
            while (isActive && socket.isOpen()) {
                writeQueue.consumeAsFlow().collect { packets ->
                    val totalBufferSize = packets.fold(0u) { acc, controlPacket ->
                        acc + controlPacket.packetSize()
                    }
                    socket.pool.borrowSuspend(totalBufferSize) { buffer ->
                        packets.forEach { packet -> packet.serialize(buffer) }
                        socket.write(buffer, keepAliveTimeout * 1.5)
                    }
                }
            }
        }
    }

    suspend fun write(controlPacket: ControlPacket) {
        try {
            writeQueue.send(listOf(controlPacket))
        } catch (e: ClosedSendChannelException) {
            // ignore closed channels
        }
    }

    suspend fun write(controlPackets: Collection<ControlPacket>) {
        try {
            writeQueue.send(controlPackets)
        } catch (e: ClosedSendChannelException) {
            // ignore closed channels
        }
    }

    suspend fun read() =
        flow {
            try {
                while (scope.isActive && socket.isOpen()) {
                    socket.read(keepAliveTimeout * 1.5) { platformBuffer, _ ->
                        lastMessageReceived = TimeSource.Monotonic.markNow()
                        readAndEmit(platformBuffer, this)
                    }
                }
            } catch (e: Exception) {
                // ignore closed exceptions
            }
        }


    private suspend fun readAndEmit(platformBuffer: PlatformBuffer, collector: FlowCollector<ControlPacket>) {
        val byte1 = platformBuffer.readUnsignedByte()
        val remainingLength = platformBuffer.readVariableByteInteger()
        process(platformBuffer, byte1, remainingLength, collector)
    }

    private suspend fun process(
        platformBuffer: PlatformBuffer,
        byte1: UByte,
        remainingLength: UInt,
        collector: FlowCollector<ControlPacket>
    ) {
        val packet = controlPacketFactory.from(platformBuffer, byte1, remainingLength)
        collector.emit(packet)
    }

    suspend fun close() {
        writeQueue.close()
        socket.close()
    }
}