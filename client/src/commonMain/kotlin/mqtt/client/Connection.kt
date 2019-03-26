package mqtt.client

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.SendChannel
import mqtt.wire4.control.packet.ConnectionRequest
import mqtt.wire4.control.packet.ControlPacket

data class ConnectionParameters(val hostname: String,
                                val port: Int, val
                                connectionRequest: ConnectionRequest,
                                val clientToBroker: Channel<ControlPacket>,
                                val brokerToClient: SendChannel<ControlPacket>)

expect fun CoroutineScope.openSocket(parameters: ConnectionParameters): Job
