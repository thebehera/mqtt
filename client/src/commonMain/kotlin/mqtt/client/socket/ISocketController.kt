package mqtt.client.socket

import kotlinx.coroutines.flow.Flow
import mqtt.wire.control.packet.ControlPacket
import kotlin.time.ExperimentalTime
import kotlin.time.TimeMark

@ExperimentalTime
interface ISocketController {
    var lastMessageReceived: TimeMark?
    suspend fun write(controlPacket: ControlPacket)

    suspend fun write(controlPackets: Collection<ControlPacket>)

    suspend fun read(): Flow<ControlPacket>

    suspend fun close()
}