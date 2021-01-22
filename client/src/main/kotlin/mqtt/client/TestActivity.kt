package mqtt.client

import android.app.Activity
import android.os.Bundle
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import mqtt.connection.ConnectionOptions
import mqtt.wire4.control.packet.ConnectionRequest

class TestActivity : Activity() {
    val mainScope = MainScope()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val context = this
        mainScope.launch(Dispatchers.Default) {
            val service = MqttAppServiceConnection.getMqttServiceConnectionAsync(context, this).await()
            val request = ConnectionRequest<Unit>("rahultest2", keepAliveSeconds = 2000, cleanSession = true)
            Log.i("RAHUL", "Pre- Add Server async")
            val port = 60_000
            val connectionId = service.addServerAsync(ConnectionOptions("10.0.2.2", port, request))
            service.publish(connectionId, "hello", System.currentTimeMillis().toString())
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        mainScope.cancel()
    }
}