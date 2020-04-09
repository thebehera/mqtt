package mqtt.client.session.transport

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.io.core.ByteReadPacket
import mqtt.buffer.BufferPool
import mqtt.buffer.SuspendCloseable
import mqtt.connection.IRemoteHost
import mqtt.wire.control.packet.ControlPacket
import kotlin.time.ExperimentalTime

interface Transport {
    suspend fun writePacket(packet: ByteReadPacket)
    suspend fun read(): ControlPacket
    fun dispose()
    suspend fun awaitClosed()
    val isClosed: Boolean
    val isWebSocket: Boolean
}

interface Transport2 : SuspendCloseable {
    suspend fun asyncWrite(controlPacket: ControlPacket)
    suspend fun incomingPackets(): Flow<ControlPacket>
}

@ExperimentalTime
expect suspend fun CoroutineScope.openMqttNetworkSession(remoteHost: IRemoteHost, pool: BufferPool): MqttTransport
