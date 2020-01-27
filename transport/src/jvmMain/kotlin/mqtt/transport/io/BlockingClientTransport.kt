package mqtt.transport.io

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.SendChannel
import kotlinx.io.core.Input
import kotlinx.io.core.readFully
import kotlinx.io.streams.asInput
import kotlinx.io.streams.writePacket
import mqtt.transport.AbstractClientControlPacketTransport
import mqtt.wire.MalformedInvalidVariableByteInteger
import mqtt.wire.MqttException
import mqtt.wire.control.packet.ControlPacket
import mqtt.wire.control.packet.IConnectionAcknowledgment
import mqtt.wire.control.packet.IConnectionRequest
import mqtt.wire.control.packet.format.ReasonCode
import mqtt.wire.data.VARIABLE_BYTE_INT_MAX
import java.io.OutputStream
import java.net.InetSocketAddress
import java.net.Socket
import kotlin.experimental.and
import kotlin.time.Duration
import kotlin.time.ExperimentalTime

@ExperimentalTime
class BlockingClientTransport(
    override val scope: CoroutineScope,
    override val connectionRequest: IConnectionRequest,
    override val maxBufferSize: Int,
    timeout: Duration
) : AbstractClientControlPacketTransport(scope, connectionRequest, maxBufferSize = maxBufferSize, timeout = timeout) {

    override var completedWrite: SendChannel<ControlPacket>? = null

    var socket: Socket? = null
    var outputStream: OutputStream? = null
    var input: Input? = null

    private var pingTimerJob: Job? = null
    private var readJob: Job? = null
    private var writeJob: Job? = null

    override suspend fun open(
        port: UShort,
        host: String?
    ): IConnectionAcknowledgment {
        val socket = Socket(host, port.toInt())
        this.socket = socket
        val outputStream = socket.getOutputStream()!!
        this.outputStream = outputStream
        write(connectionRequest, timeout)
        val inputStream = socket.getInputStream()!!
        this.input = inputStream.asInput()
        val packet = read(timeout)
        if (packet is IConnectionAcknowledgment) {
            readJob = startReadChannel()
            writeJob = startWriteChannel()
            pingTimerJob = startPingTimer()
        } else {
            throw MqttException(
                "Expected a Connection Acknowledgement, got $packet instead",
                ReasonCode.UNSUPPORTED_PROTOCOL_VERSION.byte
            )
        }
        return packet
    }

    override suspend fun read(timeout: Duration): ControlPacket {
        return input!!.read(connectionRequest.protocolVersion)
    }

    override suspend fun write(packet: ControlPacket, timeout: Duration): Int {
        val packetSeriazlied = packet.serialize()
        val count = packetSeriazlied.remaining
        outputStream?.writePacket(packetSeriazlied)
        return count.toInt()
    }

    override fun assignedPort(): UShort? {
        if (!isClosing) {
            return (socket?.remoteSocketAddress as? InetSocketAddress)?.port?.toUShort()
        }
        return null
    }

    override fun isOpen() = socket?.isConnected ?: false && !(socket?.isClosed ?: true)

    override fun close() {
        super.close()
        try {
            socket?.shutdownInput()
        } catch (e: Exception) {
        }
        try {
            input?.close()
        } catch (e: Exception) {
        }
        try {
            socket?.shutdownOutput()
        } catch (e: Exception) {
        }
        try {
            outputStream?.close()
        } catch (e: Exception) {
        }
        try {
            socket?.close()
        } catch (e: Exception) {
        }
    }
}

fun Input.read(protocolVersion: Int): ControlPacket {
    val byte1 = readByte().toUByte()
    if (!ControlPacket.isValidFirstByte(byte1)) {
        throw mqtt.wire.MalformedPacketException("Invalid MQTT Control Packet Type: $byte1 Should be in range between 0 and 15 inclusive")
    }
    val remainingLength = decodeVariableByteInteger().toInt()
    val byteArray = ByteArray(remainingLength)
    readFully(byteArray)
    return when (protocolVersion) {
        3, 4 -> mqtt.wire4.control.packet.ControlPacketV4.from(byteArray, byte1)
        5 -> mqtt.wire5.control.packet.ControlPacketV5.from(byteArray, byte1)
        else -> throw IllegalArgumentException("Received an unsupported protocol version $protocolVersion")
    }
}

fun Input.decodeVariableByteInteger(): UInt {
    var digit: Byte
    var value = 0.toUInt()
    var multiplier = 1.toUInt()
    var count = 0.toUInt()
    try {
        do {
            digit = readByte()
            count++
            value += (digit and 0x7F).toUInt() * multiplier
            multiplier *= 128.toUInt()
        } while ((digit and 0x80.toByte()).toInt() != 0)
    } catch (e: Exception) {
        throw MalformedInvalidVariableByteInteger(value)
    }
    if (value < 0.toUInt() || value > VARIABLE_BYTE_INT_MAX.toUInt()) {
        throw MalformedInvalidVariableByteInteger(value)
    }
    return value
}

//suspend fun openSocketChannel() = suspendCancellableCoroutine<SocketChannel> {
//    try {
//        it.resume(SocketChannel.open())
//    } catch (e: Exception) {
//        it.resumeWithException(e)
//    }
//}
//
//suspend fun SocketChannel.suspendConnect(socketAddress: SocketAddress) = suspendCancellableCoroutine<Boolean> {
//    try {
//        it.resume(connect(socketAddress))
//    } catch (e: Exception) {
//        it.resumeWithException(e)
//    }
//}
//
//suspend fun AbstractSelectableChannel.suspendConfigureBlocking(block: Boolean) = suspendCancellableCoroutine<SelectableChannel> {
//    try {
//        it.resume(this.configureBlocking(block))
//    } catch (e: Exception) {
//        it.resumeWithException(e)
//    }
//}
//
//suspend fun SocketChannel.suspendBind(address: SocketAddress) = suspendCancellableCoroutine<SocketChannel> {
//    try {
//        it.resume(bind(address))
//    } catch (e: Exception) {
//        it.resumeWithException(e)
//    }
//}
//
////suspend fun SocketChannel.sconnect(socketAddress: SocketAddress) = suspendCancellableCoroutine<Void> {
////    try {
////        val connected = connect(socketAddress)
////
////        it.resume(Void)
////    } catch (e: Exception) {
////
////    }
////}
//
//suspend fun SocketChannel.suspendRegister(selector: Selector, ops: Int = validOps()) = suspendCancellableCoroutine<SelectionKey> {
//    try {
//        it.resume(register(selector, ops))
//    } catch (e: Exception) {
//        it.resumeWithException(e)
//    }
//}