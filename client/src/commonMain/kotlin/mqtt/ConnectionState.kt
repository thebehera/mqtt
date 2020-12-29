package mqtt

import mqtt.wire.control.packet.IConnectionAcknowledgment
import kotlin.time.Duration
import kotlin.time.ExperimentalTime
import kotlin.time.TimeMark

sealed class ConnectionState {

    @ExperimentalTime
    class Connected(connack: IConnectionAcknowledgment, socketController: SocketController) : ConnectionState()

    object Disconnected : ConnectionState()

    @ExperimentalTime
    class Reconnecting(connectionLossException: Exception? = null, nextTryIn: Duration) : ConnectionState()
}
