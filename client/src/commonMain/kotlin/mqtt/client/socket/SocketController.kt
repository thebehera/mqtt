package mqtt.client.socket

import kotlinx.coroutines.*
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.ClosedReceiveChannelException
import kotlinx.coroutines.channels.ClosedSendChannelException
import kotlinx.coroutines.channels.SendChannel
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.consumeAsFlow
import kotlinx.coroutines.flow.flow
import mqtt.buffer.allocateNewBuffer
import mqtt.connection.IConnectionOptions
import mqtt.socket.ClientSocket
import mqtt.socket.SuspendingInputStream
import mqtt.socket.getClientSocket
import mqtt.wire.control.packet.ControlPacket
import mqtt.wire.control.packet.ControlPacketFactory
import kotlin.time.*
import kotlin.time.TimeSource.*

@ExperimentalUnsignedTypes
@ExperimentalTime
class SocketController private constructor(
    private val scope: CoroutineScope,
    private val controlPacketFactory: ControlPacketFactory,
    private val socket: ClientSocket,
    private val keepAliveTimeout: Duration
) : ISocketController {

    override var closedSocketCallback: (() -> Unit)? = null
    override var lastMessageReceived: TimeMark? = null
    override var lastMessageSent: TimeMark? = null

    private val writeQueue = Channel<Collection<ControlPacket>>(Channel.RENDEZVOUS)
    init {
        scope.launch {
            while (isActive && socket.isOpen()) {
                writeQueue.consumeAsFlow().collect { packets ->
                    val totalBufferSize = packets.fold(0u) { acc, controlPacket ->
                        acc + controlPacket.packetSize()
                    }
                    val buffer = allocateNewBuffer(totalBufferSize)
                    packets.forEach { packet ->
                        println("OUT : $packet")
                        packet.serialize(buffer)
                    }
                    try {
                        socket.write(buffer, keepAliveTimeout * 1.5)
                        lastMessageSent = Monotonic.markNow()
                    } catch (e: Exception) {
                        // connection broken
                        socket.close()
                    }
                }
            }
        }
    }

    override suspend fun write(controlPacket: ControlPacket) {
        write(listOf(controlPacket))
    }

    override suspend fun write(controlPackets: Collection<ControlPacket>) {
        try {
            writeQueue.send(controlPackets)
        } catch (e: ClosedSendChannelException) {
            // ignore closed channels
            close()
        }
    }

    override suspend fun read() = flow {
        println("start read ${socket.isOpen()}")
        val inputStream = SuspendingInputStream(keepAliveTimeout * 1.5, scope, socket)
        println("start read ${socket.isOpen()}")
        try {
            println(scope.isActive && socket.isOpen())
            while (scope.isActive && socket.isOpen()) {
                lastMessageReceived = inputStream.lastMessageReceived
                println("reading")
                val byte1 = inputStream.readUnsignedByte()
                val remainingLength = inputStream.readVariableByteInteger()
                val packet = inputStream.readTyped(remainingLength.toLong()) { readBuffer ->
                    controlPacketFactory.from(readBuffer, byte1, remainingLength)
                }
                println("IN : $packet")
                emit(packet)
            }
        } catch (e: ClosedReceiveChannelException) {
            // ignore closed
                e.printStackTrace()
            close()
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
        scope.cancel()
        closedSocketCallback?.invoke()
    }


    companion object {
        suspend fun openSocket(
            scope: CoroutineScope,
            connectionOptions: IConnectionOptions
        ): SocketController? {
            val socket = getClientSocket() ?: return null
            socket.open(port = connectionOptions.port.toUShort(), timeout = 1.seconds, hostname = connectionOptions.name)
            println("${socket.isOpen()}")
            return SocketController(
                scope,
                connectionOptions.request.controlPacketFactory,
                socket,
                connectionOptions.request.keepAliveTimeout
            )
        }
    }
}