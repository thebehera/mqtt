package mqtt.connection

import mqtt.wire.control.packet.IConnectionAcknowledgment

/**
 * Upon normal operation each mqtt session transport progresses through a sequence of states
 */
sealed class ConnectionState(val state: Int)

/**
 * Initial state of each MQTT transport. Messages may be enqueued but they won't be transmitted until the
 * transport is open.
 */
object Initializing : ConnectionState(1)

/**
 *  Starting the transport process. Messages may be enqueued but they won't be transmitted until the
 *  transport is open.
 */
object Connecting : ConnectionState(2)

/**
 *  The MQTT transport has been accepted by the server and is last known to be connected since the keep alive timeout
 */
data class Open(val acknowledgment: IConnectionAcknowledgment) : ConnectionState(state) {
    companion object {
        const val state = 3
    }
}

/**
 *  MQTT session has initiated a graceful shutdown. The session will attempt to dequeue all messages then send a
 *  DisconnectNotification
 */
object Closing : ConnectionState(4)

/**
 * The MQTT session has transmitted all of its messages and has received all messages from the peer.
 */
data class Closed(val exception: Exception? = null) : ConnectionState(state) {
    companion object {
        const val state = 5
    }
}

/**
 * The MQTT session transport to server failed. Messages that were successfully enqueued by either peer may not have been transmitted to the other.
 */
data class ConnectionFailure(val exception: Throwable) : ConnectionState(state) {
    companion object {
        const val state = 6
    }
}
