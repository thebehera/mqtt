@file:Suppress("EXPERIMENTAL_API_USAGE")

package mqtt.android_app

import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.ViewModelProviders
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import mqtt.Parcelize
import mqtt.android_app.databinding.ActivityMainBinding
import mqtt.android_app.room.initQueuedDb
import mqtt.client.connection.parameters.LogConfiguration
import mqtt.client.connection.parameters.RemoteHost
import mqtt.client.service.AndroidLogger
import mqtt.client.service.client.MqttServiceViewModel
import mqtt.wire4.control.packet.ConnectionRequest

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        initQueuedDb(this)
        super.onCreate(savedInstanceState)
        val clientService = ViewModelProviders.of(this).get(MqttServiceViewModel::class.java)
        val remoteHost = RemoteHost(
            "192.168.1.98",
            ConnectionRequest(
                "yoloswag",
                keepAliveSeconds = 4.toUShort()
            ),
            security = RemoteHost.Security(
                isTransportLayerSecurityEnabled = false
            ),
            port = 60000//.toUShort()
        )

        clientService.incomingMessageCallback { incomingControlPacket, remoteHostIdentifier ->
            Log.i("RAHUL", "IN ($remoteHostIdentifier) ACTIVITY: $incomingControlPacket")
        }

        clientService.messageSentCallback { controlPacketSent, remoteHostIdentifier ->
            Log.i("RAHUL", "OUT($remoteHostIdentifier) ACTIVITY: $controlPacketSent")
        }
        val binding = DataBindingUtil.setContentView<ActivityMainBinding>(this, R.layout.activity_main)
        binding.remoteHost = remoteHost
        binding.connectionStateView.text = "hello"
        val id = remoteHost.connectionIdentifier()
        GlobalScope
            .launch(Dispatchers.Main) {
                Log.i("RAHUL", "create connection")
                val connectionState = clientService.createConnection(remoteHost, null)
                Log.i("RAHUL", "connection created")
                binding.connectionState = connectionState
            }
    }
}


@Parcelize
object Logger :
    LogConfiguration(true, true, true, true, true) {
    override fun getLogClass(): mqtt.Log {
        return AndroidLogger()
    }
}
