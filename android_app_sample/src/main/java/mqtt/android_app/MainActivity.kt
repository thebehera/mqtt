@file:Suppress("EXPERIMENTAL_API_USAGE")

package mqtt.android_app

import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.ViewModelProviders
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import mqtt.android_app.databinding.ActivityMainBinding
import mqtt.client.connection.RemoteHost
import mqtt.client.connection.parameters.PersistableRemoteHostV4
import mqtt.wire4.control.packet.PersistableConnectionRequest

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val clientService = ViewModelProviders.of(this).get(MqttServiceViewModelGenerated::class.java)
        val remoteHost = PersistableRemoteHostV4(
            "192.168.1.98",
            PersistableConnectionRequest(
                "yjoj",
                keepAliveSeconds = 300.toUShort()
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
        GlobalScope
            .launch {
                Log.i("RAHUL", "create connection")
                val connectionState = clientService.createConnection(remoteHost)
                Log.i("RAHUL", "connection created")
                binding.connectionState = connectionState
                Log.i("RAHUL", "Subscribe")
                clientService.subscribe<SimpleModel>(remoteHost.connectionId) { topic, qos, message ->
                    println("incoming subscribe $topic, $qos, $message")
                }
                Log.i("RAHUL", "Publish")
                clientService.publish(remoteHost.connectionIdentifier(), SimpleModel("123"))
            }
    }
}