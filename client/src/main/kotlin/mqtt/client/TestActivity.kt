package mqtt.client

import android.app.Activity
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.util.Log
import androidx.databinding.DataBindingUtil
import kotlinx.coroutines.*
import mqtt.client.databinding.LayoutBinding
import mqtt.connection.ConnectionOptions
import mqtt.wire4.control.packet.ConnectionRequest
import kotlin.time.minutes

class TestActivity : Activity() {
    val mainScope = MainScope()
    var binding :LayoutBinding? = null
    var service: MqttAppServiceConnection? = null
    var connId = 0L

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        startService(Intent(this, MqttService::class.java))
        val binding = DataBindingUtil.setContentView<LayoutBinding>(this, R.layout.layout)
        this.binding = binding
        mainScope.launch {
            val service = MqttAppServiceConnection.getMqttServiceConnection(this@TestActivity, this)
            this@TestActivity.service = service
            println("get service connection activity")
            service.addIncomingMessageObserver(incomingMessageCallback)
            service.addOutgoingMessageObserver(outgoingMessageCallback)
            val request = ConnectionRequest<Unit>(Build.MODEL, cleanSession = true, keepAlive = 5.minutes)
            Log.i("RAHUL", service.findConnections().toString())
            val host = "apt.behera.me"
            connId = service.findConnections().firstOrNull { it.name == host }?.connectionId
                ?: service.addServerAsync(ConnectionOptions(host, 1883, request))


//            service.publish(connectionId, "hello", System.currentTimeMillis().toString())
        }
    }

    var hasRan = false
    private val incomingMessageCallback = object : ControlPacketCallback.Stub() {
        override fun onMessage(controlPacketWrapper: ControlPacketWrapper) {
            val binding = binding ?: return
            mainScope.launch {
                binding.incoming.append(controlPacketWrapper.packet.toString() + "\r\n")
                if (!hasRan) {
                    hasRan = true
                    launch {
                        delay(1000)
                        service?.subscribe(connId, "taco")
                    }
                }
            }
        }

        override fun onMessageFd(fileReference: String) {

        }
    }

    private val outgoingMessageCallback = object : ControlPacketCallback.Stub() {
        override fun onMessage(controlPacketWrapper: ControlPacketWrapper) {
            val binding = binding ?: return
            mainScope.launch {
                binding.outgoing.append(controlPacketWrapper.packet.toString() + "\r\n")
            }
        }

        override fun onMessageFd(fileReference: String) {

        }
    }

    override fun onDestroy() {
        super.onDestroy()
        println("destroy main")
        val serviceConnection = service
        if (serviceConnection != null) {
            unbindService(serviceConnection.serviceConnection)
        }
        binding = null
        mainScope.cancel()
    }
}