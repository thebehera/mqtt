package mqtt.client

import android.app.Activity
import android.os.Bundle
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import mqtt.buffer.allocateNewBuffer
import mqtt.connection.ConnectionOptions
import mqtt.wire4.control.packet.ConnectionRequest

class TestActivity : Activity() {
    val mainScope = MainScope()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val context = this
        mainScope.launch(Dispatchers.Default) {
            val service = MqttAppServiceConnection.getMqttServiceConnectionAsync(context, this).await()
            val request = ConnectionRequest<Unit>("rt2", cleanSession = true, keepAliveSeconds = 12)
            Log.i("RAHUL", service.findConnections().toString())
            val host = "10.0.2.2"
            val connectionId = service.findConnections().firstOrNull { it.name == host }?.connectionId
                ?: service.addServerAsync(ConnectionOptions(host, 60000, request))

            service.publish(connectionId, "hello", System.currentTimeMillis().toString())
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        mainScope.cancel()
    }
}