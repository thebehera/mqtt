package mqtt.client

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.SendChannel
import mqtt.wire.control.packet.ControlPacket
import mqtt.wire.control.packet.IConnectionRequest
import kotlin.coroutines.CoroutineContext

data class ConnectionParameters(val hostname: String,
                                val port: Int,
                                val connectionRequest: IConnectionRequest,
                                val reconnectIfNetworkLost: Boolean = true,
                                val clientToBroker: Channel<ControlPacket> = Channel(),
                                val brokerToClient: SendChannel<ControlPacket> = Channel())


interface IConnection : CoroutineScope {
    val parameters: ConnectionParameters
    val job: Job
    val dispatcher: CoroutineDispatcher
    override val coroutineContext: CoroutineContext get() = dispatcher + job
    fun lastMessageBetweenClientAndServer(): Long
    fun isConnectedOrConnecting(): Boolean
    fun start(): Job
}


expect class Connection(parameters: ConnectionParameters) : IConnection
