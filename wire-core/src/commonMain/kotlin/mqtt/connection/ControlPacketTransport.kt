package mqtt.connection

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.ReceiveChannel
import kotlinx.coroutines.channels.SendChannel
import kotlinx.coroutines.flow.Flow
import mqtt.wire.control.packet.ControlPacket
import mqtt.wire.control.packet.IConnectionAcknowledgment
import mqtt.wire.control.packet.IConnectionRequest
import kotlin.time.Duration
import kotlin.time.ExperimentalTime
import kotlin.time.seconds

@ExperimentalTime
interface ControlPacketTransport {
    val scope: CoroutineScope
    val connectionRequest: IConnectionRequest
    val maxBufferSize: Int
    val outboundChannel: SendChannel<ControlPacket>
    val inboxChannel: ReceiveChannel<ControlPacket>
    val incomingControlPackets: Flow<ControlPacket>
    var completedWrite: Channel<ControlPacket>?
    fun assignedPort(): UShort?
    fun isOpen(): Boolean

    suspend fun suspendClose()
    fun close()
}


@ExperimentalTime
interface ClientControlPacketTransport : ControlPacketTransport {
    suspend fun open(port: UShort, host: String? = null): IConnectionAcknowledgment
}

@ExperimentalTime
interface ServerControlPacketTransport {
    val scope: CoroutineScope
    fun port(): UShort?
    suspend fun listen(
        port: UShort? = null,
        host: String = "127.0.0.1",
        readTimeout: Duration = 1.seconds
    ): Flow<ControlPacketTransport>

    fun close()
}