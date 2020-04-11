package mqtt.client.session.transport

import kotlinx.coroutines.flow.Flow
import mqtt.buffer.SuspendCloseable
import mqtt.wire.control.packet.ControlPacket

interface Transport : SuspendCloseable {
    suspend fun asyncWrite(controlPacket: ControlPacket)
    suspend fun incomingPackets(): Flow<ControlPacket>
}