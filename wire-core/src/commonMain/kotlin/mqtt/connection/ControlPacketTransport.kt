package mqtt.connection

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.channels.SendChannel
import kotlinx.coroutines.flow.Flow
import kotlinx.io.core.Closeable
import mqtt.wire.control.packet.ControlPacket
import mqtt.wire.control.packet.IConnectionAcknowledgment
import mqtt.wire.control.packet.IConnectionRequest

interface ControlPacketTransport : Closeable {
    val connectionRequest: IConnectionRequest
    val scope: CoroutineScope
    val maxBufferSize: Int
    val outboundChannel: SendChannel<ControlPacket>
    val incomingControlPackets: Flow<ControlPacket>
}


interface ClientControlPacketTransport : ControlPacketTransport {
    suspend fun open(port: Int, host: String? = null): IConnectionAcknowledgment
}