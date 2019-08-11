package mqtt.client.service.ipc

import android.os.Handler
import android.os.Message
import mqtt.client.connection.parameters.IMqttConfiguration

class EventHandler(private val listener: OnRemoteCommandListener) : Handler() {
    override fun handleMessage(msg: Message) {
        super.handleMessage(msg)
        when (msg.what) {
            Command.QUEUE_INSERTED.ordinal -> listener.onQueueInvalidated()
            Command.CONNECT.ordinal -> forwardConnect(msg)
        }
    }

    private fun forwardConnect(msg: Message) {
        val connectionParameters = msg.obj as? IMqttConfiguration
        if (connectionParameters == null) {
            println("failed to read connection parameters")
            return
        }
        listener.connect(connectionParameters)
    }
}