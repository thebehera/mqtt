package mqtt.client.session.transport

import kotlinx.coroutines.flow.Flow
import kotlinx.io.core.ByteReadPacket
import mqtt.buffer.SuspendCloseable
import mqtt.wire.control.packet.ControlPacket

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