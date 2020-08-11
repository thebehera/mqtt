@file:Suppress("EXPERIMENTAL_API_USAGE")

package mqtt.client.session.transport

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.isActive
import mqtt.buffer.BufferPool
import mqtt.connection.IRemoteHost
import mqtt.socket.ClientToServerSocket
import mqtt.socket.getClientSocket
import mqtt.wire.control.packet.ControlPacket
import mqtt.wire.control.packet.ControlPacketReader
import mqtt.wire.control.packet.IConnectionAcknowledgment
import mqtt.wire.control.packet.format.fixed.DirectionOfFlow
import kotlin.time.*

@ExperimentalTime
class MqttTransport private constructor(
    private val scope: CoroutineScope,
    private val pool: BufferPool,
    remoteHost: IRemoteHost,
    val socket: ClientToServerSocket,
    private val controlPacketReader: ControlPacketReader
) {
    private val keepAliveTimeout = remoteHost.request.keepAliveTimeoutSeconds.toInt().toDuration(DurationUnit.SECONDS)
    lateinit var lastMessageReceived: TimeMark
    private lateinit var lastMessageSent: TimeMark

    suspend fun asyncWrite(controlPacket: ControlPacket) {
        if (controlPacket.direction == DirectionOfFlow.SERVER_TO_CLIENT) {
            throw IllegalArgumentException("Server to client message")
        }
        pool.borrowSuspend { buffer ->
            controlPacket.serialize(buffer)
            socket.write(buffer, keepAliveTimeout)
            lastMessageSent = TimeSource.Monotonic.markNow()
        }
    }

    private suspend fun readPacket(): ControlPacket {
        val packet = socket.readTyped(keepAliveTimeout) {
            controlPacketReader.from(it)
        }
        lastMessageReceived = TimeSource.Monotonic.markNow()
        return packet
    }

    suspend fun incomingPackets() = flow {
        while (isOpen()) {
            emit(readPacket())
        }
    }

    fun isOpen() = socket.isOpen() && scope.isActive

    suspend fun close() = socket.close()

    companion object {
        suspend fun openConnection(
            scope: CoroutineScope,
            remoteHost: IRemoteHost,
            pool: BufferPool
        ): ConnectedMqttTransport {
            val clientSocket = getClientSocket()
            clientSocket.open(
                remoteHost.port.toUShort(),
                remoteHost.connectionTimeout.toDuration(DurationUnit.MILLISECONDS),
                remoteHost.name
            )
            val session = MqttTransport(scope, pool, remoteHost, clientSocket, remoteHost.request.controlPacketReader)
            session.asyncWrite(remoteHost.request)
            val connack = session.readPacket() as IConnectionAcknowledgment
            return ConnectedMqttTransport(session, connack)
        }
    }
}

@ExperimentalTime
data class ConnectedMqttTransport(val transport: MqttTransport, val connack: IConnectionAcknowledgment)