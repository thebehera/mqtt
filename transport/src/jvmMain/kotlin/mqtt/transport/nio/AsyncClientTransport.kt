package mqtt.transport.nio

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import mqtt.connection.ClientControlPacketTransport
import mqtt.transport.nio.util.aClose
import mqtt.transport.nio.util.aConnect
import mqtt.transport.nio.util.asInetAddress
import mqtt.wire.MqttException
import mqtt.wire.control.packet.IConnectionAcknowledgment
import mqtt.wire.control.packet.IConnectionRequest
import mqtt.wire.control.packet.format.ReasonCode
import java.net.InetSocketAddress
import java.nio.channels.AsynchronousSocketChannel
import kotlin.time.ExperimentalTime
import kotlin.time.seconds

@ExperimentalTime
class AsyncClientTransport(
    override val scope: CoroutineScope,
    override val socket: AsynchronousSocketChannel,
    connectionReq: IConnectionRequest,
    override val maxBufferSize: Int
) : AsyncBaseClientTransport(
    scope, socket, connectionReq, maxBufferSize,
    connectionReq.keepAliveTimeoutSeconds.toLong().seconds
), ClientControlPacketTransport {

    private var pingTimerJob: Job? = null
    private var readJob: Job? = null
    private var writeJob: Job? = null

    override suspend fun open(port: UShort, host: String?): IConnectionAcknowledgment {
        val socketAddress = InetSocketAddress(host.asInetAddress(), port.toInt())
        socket.aConnect(socketAddress)
        write(connectionRequest, timeout)
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

    override suspend fun suspendClose() {
        try {
            super.suspendClose()
        } finally {
            socket.aClose()
            pingTimerJob?.cancel()
            readJob?.cancel()
            writeJob?.cancel()
        }
    }

}
