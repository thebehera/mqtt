package mqtt.transport

import kotlinx.coroutines.CoroutineScope
import mqtt.connection.ClientControlPacketTransport
import mqtt.wire.control.packet.IConnectionRequest
import kotlin.time.ExperimentalTime

@ExperimentalTime
expect suspend fun aMqttClient(
    scope: CoroutineScope,
    connectionRequest: IConnectionRequest,
    maxBufferSize: Int,
    group: Any?
): ClientControlPacketTransport