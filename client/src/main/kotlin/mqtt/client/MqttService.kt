package mqtt.client

import android.app.Service
import android.content.Intent
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.cancel

class MqttService : Service() {
    private val mainScope = MainScope()

    val binder = object : IRemoteMqttService.Stub() {
        override fun addServer() {

        }

        override fun removeServer() {

        }

        override fun connect(connackCallback: ControlPacketCallback) {

        }


    }

    override fun onBind(intent: Intent) = binder

    override fun onDestroy() {
        mainScope.cancel()
        super.onDestroy()
    }
}
