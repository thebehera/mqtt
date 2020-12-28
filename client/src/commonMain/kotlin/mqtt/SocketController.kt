package mqtt

import kotlinx.coroutines.*
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.ClosedReceiveChannelException
import kotlinx.coroutines.channels.ClosedSendChannelException
import kotlinx.coroutines.flow.FlowCollector
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.consumeAsFlow
import kotlinx.coroutines.flow.flow
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
    val scope: CoroutineScope,
    val controlPacketFactory: ControlPacketFactory,
    val socket: ClientSocket,
    val keepAliveTimeout: Duration
) {
    private val writeQueue = Channel<Collection<ControlPacket>>(Channel.RENDEZVOUS)
    lateinit var lastMessageReceived: TimeMark

    init {
        scope.launch(Dispatchers.Default) {
            while (isActive && socket.isOpen()) {
                writeQueue.consumeAsFlow().collect { packets ->
                    socket.write(packets, keepAliveTimeout * 1.5)
                }
            }
        }
    }

    suspend fun write(controlPacket: ControlPacket) {
        try {
            writeQueue.send(listOf(controlPacket))
        } catch (e: ClosedSendChannelException) {
            println("closed while trying to write $controlPacket")
            // ignore closed channels
        }
    }

    suspend fun write(controlPackets: Collection<ControlPacket>) {
        try {
            writeQueue.send(controlPackets)
        } catch (e: ClosedSendChannelException) {
            // ignore closed channels
            println("closed while trying to write $controlPackets")
        }
    }

    suspend fun read() = withContext(Dispatchers.Default) {
        flow {
            try {
                while (scope.isActive && socket.isOpen()) {
                    socket.read(keepAliveTimeout * 1.5) { platformBuffer, _ ->
                        lastMessageReceived = TimeSource.Monotonic.markNow()
                        readAndEmit(platformBuffer, this)
                    }
                }
            } catch (e: ClosedReceiveChannelException) {
                // ignore closed exceptions
//                println("closed read")
            }

        }
    }

    private suspend fun readAndEmit(platformBuffer: PlatformBuffer, collector: FlowCollector<ControlPacket>) {
//        if (!platformBuffer.hasRemaining()) {
//            socket.read(keepAliveTimeout * 1.5) { nextBuffer, _ ->
//                readAndEmit(nextBuffer, collector)
//            }
//            return
//        }
        val byte1 = platformBuffer.readUnsignedByte()
        val remainingLength = platformBuffer.readVariableByteInteger()
        process(platformBuffer, byte1, remainingLength, collector)
//        when(val remainingLengthTrial = platformBuffer.tryReadingVariableByteInteger()) {
//            is ReadBuffer.VariableByteIntegerRead.NotEnoughSpaceInBuffer -> {
//                socket.read { nextBuffer, _ ->
//                    process(nextBuffer, byte1, remainingLengthTrial.getRemainingLengthWithNextBuffer(nextBuffer).remainingLength, collector)
//                }
//            }
//            is ReadBuffer.VariableByteIntegerRead.SuccessfullyRead -> {
//
//            }
//        }
    }

    suspend fun process(
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