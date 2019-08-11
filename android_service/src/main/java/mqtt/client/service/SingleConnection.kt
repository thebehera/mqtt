package mqtt.client.service

import android.app.Service
import android.content.Intent
import android.os.Messenger
import mqtt.client.connection.parameters.IMqttConfiguration
import mqtt.client.service.ipc.EventHandler
import mqtt.client.service.ipc.OnRemoteCommandListener

class SingleConnection : Service(), OnRemoteCommandListener {
    private val messenger = Messenger(EventHandler(this))
    override fun onBind(intent: Intent) = messenger.binder!!

    override fun connect(connectionParameters: IMqttConfiguration) {

    }

    override fun onQueueInvalidated() {

    }
}