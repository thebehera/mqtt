package mqtt.client.service

import android.content.Intent
import android.os.Messenger
import mqtt.client.service.ipc.EventHandler

class SingleConnection : CoroutineService() {
    private val mqttConnectionManager: OnConnectionListener  by lazy { ConnectionManager() }
    private val queueManager: OnQueueInvalidatedListener by lazy { ConnectedQueueManager() }
    private val messenger by lazy { Messenger(EventHandler(mqttConnectionManager, queueManager)) }
    override fun onBind(intent: Intent) = messenger.binder!!
}
