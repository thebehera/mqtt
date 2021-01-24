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
            val request = ConnectionRequest<Unit>("rahultest2", keepAliveSeconds = 2000, cleanSession = true)
            val size = request.packetSize()
            val buffer = allocateNewBuffer(size)
            request.serialize(buffer)
            buffer.resetForRead()
            val request2 = request.controlPacketFactory.from(buffer)
            check(request.toString().contentEquals(request2.toString()))
            Log.i("RAHUL", "Pre- Add Server async")
            val port = 1883
            val connectionId = service.addServerAsync(ConnectionOptions("apt.behera.me", port, request))
            service.publish(connectionId, "hello", System.currentTimeMillis().toString())
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        mainScope.cancel()
    }
}