package mqtt.client.service.ipc

import android.os.Message
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import mqtt.client.service.OnQueueInvalidatedListener
import mqtt.client.service.OnRemoteIpcCommand
import mqtt.connection.IMqttConfiguration
import mqtt.persistence.IQueuedMessage
import mqtt.persistence.MqttPersistence
import kotlin.coroutines.CoroutineContext

class EventHandler(
    override val coroutineContext: CoroutineContext,
    private val connection: OnRemoteIpcCommand,
    private val queue: OnQueueInvalidatedListener? = null
) : CoroutineScope {
    fun handleMessage(msg: Message) {
        val data = msg.data
        val config = data.getParcelable<IMqttConfiguration>("config")
        val obj = msg.obj
        launch {
            when (msg.what) {
                BoundClientToService.QUEUE_INSERTED.ordinal -> queue?.onQueueInvalidated(msg.obj as MqttPersistence<out IQueuedMessage>)
                BoundClientToService.CREATE_CONNECTION.ordinal -> forwardConnect(msg)
            }
            if (obj != null) {
                obj.toString()
            }
            if (data != null) {
                data.toString()
            }
            if (config != null) {
                config.toString()
            }
        }
    }

    private suspend fun forwardConnect(msg: Message) {
        val config = (msg.obj as? IMqttConfiguration)
        connection.connectOrDisconnect(config)
    }
}
