@file:Suppress("EXPERIMENTAL_API_USAGE")

package mqtt.client

import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import mqtt.buffer.BufferPool
import mqtt.connection.IRemoteHost
import mqtt.socket.ClientToServerSocket
import mqtt.socket.getClientSocket
import mqtt.wire.control.packet.ControlPacket
import mqtt.wire.control.packet.ControlPacketReader
import mqtt.wire.control.packet.format.fixed.DirectionOfFlow
import kotlin.time.DurationUnit
import kotlin.time.ExperimentalTime
import kotlin.time.toDuration

@ExperimentalTime
class MqttNetworkSession private constructor(
    private val pool: BufferPool,
    val remoteHost: IRemoteHost,
    val socket: ClientToServerSocket
) {
    private val writeMutex = Mutex()
    private val keepAliveTimeout = remoteHost.request.keepAliveTimeoutSeconds.toInt().toDuration(DurationUnit.SECONDS)

    suspend fun asyncWrite(controlPacket: ControlPacket) {
        if (controlPacket.direction == DirectionOfFlow.SERVER_TO_CLIENT) {
            throw IllegalArgumentException("Server to client message")
        }
        pool.borrowSuspend { buffer ->
            controlPacket.serialize(buffer)
            writeMutex.withLock {
                socket.write(buffer, keepAliveTimeout)
            }
        }
    }

    suspend fun incomingPackets(controlPacketReader: ControlPacketReader) = flow {
        while (socket.isOpen()) {
            pool.borrowSuspend { buffer ->
                socket.read(buffer, keepAliveTimeout)
                val packet = controlPacketReader.from(buffer)
                emit(packet)
            }
        }
    }

    companion object {
        suspend fun openConnection(remoteHost: IRemoteHost, pool: BufferPool): MqttNetworkSession {
            val clientSocket = getClientSocket()
            clientSocket.open(
                remoteHost.connectionTimeout.toDuration(DurationUnit.MILLISECONDS),
                remoteHost.port.toUShort(),
                remoteHost.name
            )
            return MqttNetworkSession(pool, remoteHost, clientSocket)
        }
    }
}