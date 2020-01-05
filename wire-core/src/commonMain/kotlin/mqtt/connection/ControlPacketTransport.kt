package mqtt.connection

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.channels.SendChannel
import kotlinx.coroutines.flow.Flow
import kotlinx.io.core.Closeable
import mqtt.wire.control.packet.ControlPacket
import mqtt.wire.control.packet.IConnectionAcknowledgment
import mqtt.wire.control.packet.IConnectionRequest

interface ControlPacketTransport : Closeable {
    val scope: CoroutineScope
    val maxBufferSize: Int
    val outboundChannel: SendChannel<ControlPacket>
    val incomingControlPackets: Flow<ControlPacket>
    var completedWrite: SendChannel<ControlPacket>?
    fun assignedPort(): UShort?
}


interface ClientControlPacketTransport : ControlPacketTransport {
    val connectionRequest: IConnectionRequest
    suspend fun open(port: UShort, host: String? = null): IConnectionAcknowledgment
}

interface ServerControlPacketTransport : Closeable {
    val scope: CoroutineScope
    suspend fun listen(port: UShort? = null, host: String = "127.0.0.1"): Flow<ControlPacketTransport>
}