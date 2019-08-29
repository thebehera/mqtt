package mqtt.client.service

import android.content.Intent
import android.os.Handler
import android.os.Messenger

class SingleConnection : CoroutineService() {
    //    private var mqttConnectionManager: OnConnectionListener?
//    private val queueManager: OnQueueInvalidatedListener by lazy { ConnectedQueueManager(coroutineContext) }
    private val messenger by lazy {
        Messenger(
            Handler()
//    EventHandler(mqttConnectionManager, queueManager)
        )
    }
    override fun onBind(intent: Intent) = messenger.binder!!


}
