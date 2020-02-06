package mqtt.transport

import kotlinx.coroutines.CoroutineScope
import mqtt.connection.ClientControlPacketTransport
import mqtt.transport.io.BlockingClientTransport
import mqtt.transport.nio2.AsyncClientTransport
import mqtt.transport.nio2.util.asyncSocket
import mqtt.wire.control.packet.IConnectionRequest
import kotlin.time.Duration
import kotlin.time.ExperimentalTime

@ExperimentalTime
actual suspend fun aMqttClient(
    scope: CoroutineScope,
    connectionRequest: IConnectionRequest,
    maxBufferSize: Int,
    group: Any?
): ClientControlPacketTransport {
    return AsyncClientTransport(scope, asyncSocket(), connectionRequest, maxBufferSize)
}

@ExperimentalTime
actual suspend fun blockingMqttClient(
    scope: CoroutineScope,
    connectionRequest: IConnectionRequest,
    maxBufferSize: Int,
    timeout: Duration
): ClientControlPacketTransport = BlockingClientTransport(scope, connectionRequest, maxBufferSize, timeout)