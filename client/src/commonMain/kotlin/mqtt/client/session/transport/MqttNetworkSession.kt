@file:Suppress("EXPERIMENTAL_API_USAGE")

package mqtt.client.session.transport

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import mqtt.buffer.BufferPool
import mqtt.connection.IRemoteHost
import mqtt.socket.ClientToServerSocket
import mqtt.socket.getClientSocket
import mqtt.wire.control.packet.ControlPacket
import mqtt.wire.control.packet.ControlPacketReader
import mqtt.wire.control.packet.format.fixed.DirectionOfFlow
import kotlin.time.*

@ExperimentalTime
class MqttNetworkSession private constructor(
    private val pool: BufferPool,
    val remoteHost: IRemoteHost,
    val socket: ClientToServerSocket,
    val controlPacketReader: ControlPacketReader
) : Transport2 {
    private val writeMutex = Mutex()
    private val keepAliveTimeout = remoteHost.request.keepAliveTimeoutSeconds.toInt().toDuration(DurationUnit.SECONDS)
    private lateinit var lastMessageReceived: ClockMark
    private lateinit var lastMessageSent: ClockMark

    override suspend fun asyncWrite(controlPacket: ControlPacket) {
        if (controlPacket.direction == DirectionOfFlow.SERVER_TO_CLIENT) {
            throw IllegalArgumentException("Server to client message")
        }
        pool.borrowSuspend { buffer ->
            controlPacket.serialize(buffer)
            writeMutex.withLock {
                socket.write(buffer, keepAliveTimeout)
                lastMessageSent = MonoClock.markNow()
            }
        }
    }

    override suspend fun incomingPackets(scope: CoroutineScope) = flow {
        while (socket.isOpen() && scope.isActive) {
            pool.borrowSuspend { buffer ->
                socket.read(buffer, keepAliveTimeout)
                val packet = controlPacketReader.from(buffer)
                lastMessageReceived = MonoClock.markNow()
                emit(packet)
            }
        }
    }

    private fun startKeepAlive(scope: CoroutineScope) = scope.launch {
        while (socket.isOpen() && scope.isActive) {
            delayUntilKeepAlive()
            asyncWrite(controlPacketReader.pingRequest())
        }
    }

    private suspend fun delayUntilKeepAlive() {
        val keepAliveDuration = remoteHost.request.keepAliveTimeoutSeconds.toInt().toDuration(DurationUnit.SECONDS)
        while (lastMessageReceived.elapsedNow() > keepAliveDuration) {
            delay((lastMessageReceived.elapsedNow() - keepAliveDuration).toLongMilliseconds())
        }
    }

    override suspend fun close() = socket.close()

    companion object {
        suspend fun openConnection(remoteHost: IRemoteHost, pool: BufferPool): MqttNetworkSession {
            val clientSocket = getClientSocket()
            clientSocket.open(
                remoteHost.connectionTimeout.toDuration(DurationUnit.MILLISECONDS),
                remoteHost.port.toUShort(),
                remoteHost.name
            )
            val session = MqttNetworkSession(pool, remoteHost, clientSocket)
            session.asyncWrite(remoteHost.request)
            session.startKeepAlive()
            return session
        }
    }
}