package mqtt.client

/**
 * Upon normal operation each mqtt client connection progresses through a sequence of states
 */
sealed class ConnectionState

/**
 * Initial state of each MQTT connection. Messages may be enqueued but they won't be transmitted until the
 * connection is open.
 */
object Initializing : ConnectionState()

/**
 *  Starting the connection process. Messages may be enqueued but they won't be transmitted until the
 *  connection is open.
 */
object Connecting : ConnectionState()

/**
 *  The MQTT connection has been accepted by the server and is last known to be connected since the keep alive timeout
 */
object Open : ConnectionState()

/**
 *  MQTT client has initiated a graceful shutdown. The client will attempt to dequeue all messages then send a
 *  DisconnectNotification
 */
object Closing : ConnectionState()

/**
 * The MQTT client has transmitted all of its messages and has received all messages from the peer.
 */
data class Closed(val exception: Exception? = null) : ConnectionState()

/**
 * The MQTT client connection to server failed. Messages that were successfully enqueued by either peer may not have been transmitted to the other.
 */
data class ConnectionFailure(val exception: Exception) : ConnectionState()
