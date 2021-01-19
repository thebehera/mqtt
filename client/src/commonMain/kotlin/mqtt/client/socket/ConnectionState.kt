package mqtt.client.socket

import mqtt.wire.control.packet.IConnectionAcknowledgment
import kotlin.time.Duration
import kotlin.time.ExperimentalTime

sealed class ConnectionState {

    @ExperimentalTime
    class Connected(connack: IConnectionAcknowledgment, socketController: ISocketController) : ConnectionState()

    object Disconnected : ConnectionState()

    @ExperimentalTime
    class Reconnecting(connectionLossException: Exception? = null, nextTryIn: Duration) : ConnectionState()
}
