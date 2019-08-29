package mqtt.client.service.ipc

import android.os.Handler
import android.os.Message
import mqtt.client.service.OnConnectionListener
import mqtt.client.service.OnQueueInvalidatedListener
import mqtt.connection.IMqttConfiguration
import mqtt.persistence.IQueuedMessage
import mqtt.persistence.MqttPersistence

class EventHandler(
    private val connection: OnConnectionListener,
    private val queue: OnQueueInvalidatedListener? = null
) :
    Handler() {
    override fun handleMessage(msg: Message) {
        super.handleMessage(msg)
        when (msg.what) {
            Command.QUEUE_INSERTED.ordinal -> queue?.onQueueInvalidated(msg.obj as MqttPersistence<out IQueuedMessage>)
            Command.CONNECT.ordinal -> forwardConnect(msg)
        }
    }

    private fun forwardConnect(msg: Message) {
        val connectionParameters = msg.obj as? IMqttConfiguration
        if (connectionParameters == null) {
            println("failed to read connection parameters")
            return
        }
        connection.connect(connectionParameters)
    }
}
