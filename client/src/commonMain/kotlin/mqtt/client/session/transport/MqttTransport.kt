@file:Suppress("EXPERIMENTAL_API_USAGE")

package mqtt.client.session.transport

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import mqtt.buffer.BufferPool
import mqtt.buffer.PlatformBuffer
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
    val remoteHost: IRemoteHost,
    val socket: ClientToServerSocket,
    val controlPacketReader: ControlPacketReader
) : Transport {
    private val writeMutex = Mutex()
    private val keepAliveTimeout = remoteHost.request.keepAliveTimeoutSeconds.toInt().toDuration(DurationUnit.SECONDS)
    lateinit var lastMessageReceived: TimeMark
    lateinit var lastMessageSent: TimeMark

    override suspend fun asyncWrite(controlPacket: ControlPacket) {
        if (controlPacket.direction == DirectionOfFlow.SERVER_TO_CLIENT) {
            throw IllegalArgumentException("Server to client message")
        }
        pool.borrowSuspend { buffer ->
            controlPacket.serialize(buffer)
            writeMutex.withLock {
                socket.write(buffer, keepAliveTimeout)
                lastMessageSent = TimeSource.Monotonic.markNow()
            }
        }
    }

    private suspend fun readPacket(buffer: PlatformBuffer): ControlPacket {
        socket.read(buffer, keepAliveTimeout)
        val packet = controlPacketReader.from(buffer)
        lastMessageReceived = TimeSource.Monotonic.markNow()
        return packet
    }

    override suspend fun incomingPackets() = flow {
        while (isOpen()) {
            pool.borrowSuspend { buffer ->
                emit(readPacket(buffer))
            }
        }
    }

    fun isOpen() = socket.isOpen() && scope.isActive

    override suspend fun close() = socket.close()

    companion object {
        suspend fun openConnection(
            scope: CoroutineScope,
            remoteHost: IRemoteHost,
            pool: BufferPool
        ): ConnectedMqttTransport {
            val clientSocket = getClientSocket()
            clientSocket.open(
                remoteHost.connectionTimeout.toDuration(DurationUnit.MILLISECONDS),
                remoteHost.port.toUShort(),
                remoteHost.name
            )
            val session = MqttTransport(scope, pool, remoteHost, clientSocket, remoteHost.request.controlPacketReader)
            session.asyncWrite(remoteHost.request)
            val connack = pool.borrowSuspend {
                session.readPacket(it) as IConnectionAcknowledgment
            }
            return ConnectedMqttTransport(session, connack)
        }
    }
}

@ExperimentalTime
data class ConnectedMqttTransport(val transport: MqttTransport, val connack: IConnectionAcknowledgment)