@file:Suppress("EXPERIMENTAL_API_USAGE")

package mqtt.android_app

import android.os.Build
import android.os.Bundle
import android.preference.PreferenceManager
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.ViewModelProviders
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import mqtt.android_app.databinding.ActivityMainBinding
import mqtt.client.RemoteHost
import mqtt.client.persistence.PersistableRemoteHostV4
import mqtt.wire4.control.packet.ConnectionRequest
import java.util.*
import kotlin.time.ExperimentalTime

@ExperimentalTime
class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val prefs = PreferenceManager.getDefaultSharedPreferences(this)
        val clientIdTmp = prefs.getString("clientId", null)
        val clientId = clientIdTmp ?: "${Build.MODEL}-${Build.DEVICE}-${UUID.randomUUID()}"
        if (clientIdTmp == null) {
            prefs.edit().putString("clientId", clientId).apply()
        }
        val clientService = ViewModelProviders.of(this).get(MqttServiceViewModelGenerated::class.java)
        val remoteHost = PersistableRemoteHostV4(
            BuildConfig.IPV4[0],
            ConnectionRequest(
                clientId,
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