package mqtt.client.connection

import mqtt.wire.control.packet.IConnectionAcknowledgment

/**
 * Upon normal operation each mqtt session transport progresses through a sequence of states
 */
sealed class ConnectionState

/**
 * Initial state of each MQTT transport. Messages may be enqueued but they won't be transmitted until the
 * transport is open.
 */
object Initializing : ConnectionState()

/**
 *  Starting the transport process. Messages may be enqueued but they won't be transmitted until the
 *  transport is open.
 */
object Connecting : ConnectionState()

/**
 *  The MQTT transport has been accepted by the server and is last known to be connected since the keep alive timeout
 */
data class Open(val acknowledgment: IConnectionAcknowledgment) : ConnectionState()

/**
 *  MQTT session has initiated a graceful shutdown. The session will attempt to dequeue all messages then send a
 *  DisconnectNotification
 */
object Closing : ConnectionState()

/**
 * The MQTT session has transmitted all of its messages and has received all messages from the peer.
 */
data class Closed(val exception: Exception? = null) : ConnectionState()

/**
 * The MQTT session transport to server failed. Messages that were successfully enqueued by either peer may not have been transmitted to the other.
 */
data class ConnectionFailure(val exception: Exception) : ConnectionState()
