package mqtt.client

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.ClosedReceiveChannelException
import kotlinx.coroutines.channels.ClosedSendChannelException
import kotlinx.coroutines.channels.SendChannel
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.consumeAsFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import mqtt.buffer.allocateNewBuffer
import mqtt.connection.IConnectionOptions
import mqtt.socket.ClientSocket
import mqtt.socket.SuspendingInputStream
import mqtt.socket.getClientSocket
import mqtt.wire.control.packet.ControlPacket
import mqtt.wire.control.packet.ControlPacketFactory
import kotlin.time.Duration
import kotlin.time.ExperimentalTime
import kotlin.time.TimeMark

@ExperimentalUnsignedTypes
@ExperimentalTime
class SocketController private constructor(
    private val scope: CoroutineScope,
    private val controlPacketFactory: ControlPacketFactory,
    private val socket: ClientSocket,
    private val keepAliveTimeout: Duration,
    private val writeQueue: SendChannel<Collection<ControlPacket>>,
) : ISocketController {

    override var lastMessageReceived: TimeMark? = null

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
        val inputStream = SuspendingInputStream(keepAliveTimeout * 1.5, scope, socket)
        try {
            while (scope.isActive && socket.isOpen()) {
                lastMessageReceived = inputStream.lastMessageReceived
                val byte1 = inputStream.readUnsignedByte()
                val remainingLength = inputStream.readVariableByteInteger()
                val packet = inputStream.readTyped(remainingLength.toLong()) { readBuffer ->
                    controlPacketFactory.from(readBuffer, byte1, remainingLength)
                }
                emit(packet)
            }
        } catch (e: ClosedReceiveChannelException) {
            // ignore closed
        } finally {
            inputStream.close()
        }
    }

    override suspend fun close() {
        writeQueue.close()
        try {
            socket.close()
        } catch (e: Exception) {
            // ignore close exceptions
        }
    }


    companion object {
        suspend fun openSocket(
            scope: CoroutineScope,
            connectionOptions: IConnectionOptions
        ): SocketController? {
            println("get client socket")
            val socket = getClientSocket() ?: return null
            println("opening")
            socket.open(port = connectionOptions.port.toUShort(), hostname = connectionOptions.name)
            println("opened")
            val writeQueue = Channel<Collection<ControlPacket>>(Channel.RENDEZVOUS)
            scope.launch {
                while (isActive && socket.isOpen()) {
                    writeQueue.consumeAsFlow().collect { packets ->
                        val totalBufferSize = packets.fold(0u) { acc, controlPacket ->
                            acc + controlPacket.packetSize()
                        }
                        val buffer = allocateNewBuffer(totalBufferSize)
                        packets.forEach { packet -> packet.serialize(buffer) }
                        socket.write(buffer, connectionOptions.request.keepAliveTimeout * 1.5)
                    }
                }
            }
            println("return")
            return SocketController(
                scope,
                connectionOptions.request.controlPacketFactory,
                socket,
                connectionOptions.request.keepAliveTimeout,
                writeQueue
            )
        }
    }
}