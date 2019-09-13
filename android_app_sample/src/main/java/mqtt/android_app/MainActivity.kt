package mqtt.android_app

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.ViewModelProviders
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import mqtt.Log
import mqtt.Parcelize
import mqtt.android_app.databinding.ActivityMainBinding
import mqtt.android_app.room.AndroidConfiguration
import mqtt.android_app.room.initQueuedDb
import mqtt.client.connection.parameters.LogConfiguration
import mqtt.client.connection.parameters.RemoteHost
import mqtt.client.service.client.MqttServiceViewModel
import mqtt.wire4.control.packet.ConnectionRequest

class MainActivity : AppCompatActivity() {
    //    val client by lazy { ViewModelProviders.of(this).get(SimpleMqttClientViewModel::class.java) }
    val clientService by lazy { ViewModelProviders.of(this).get(MqttServiceViewModel::class.java) }
    override fun onCreate(savedInstanceState: Bundle?) {
        initQueuedDb(this)
        super.onCreate(savedInstanceState)
        clientService
        val config = AndroidConfiguration(
            RemoteHost(
                "192.168.1.98",
                ConnectionRequest(
                    "yoloswag",
                    keepAliveSeconds = 4.toUShort()
                ),
                security = RemoteHost.Security(
                    isTransportLayerSecurityEnabled = false
                ),
                port = 60000.toUShort()
            ),
            Logger
        )
        val binding = DataBindingUtil.setContentView<ActivityMainBinding>(this, R.layout.activity_main)
//        val connection = client.connectAsync(config)
        binding.remoteHost = config.remoteHost
        GlobalScope
            .launch {
                val connectionState = clientService.createConnection(config)
                binding.connectionState = connectionState
//                connection.await()
//                println("subscribe")
//                client.subscribe<String>("helloyolo", QualityOfService.AT_LEAST_ONCE) { topic, qos, message ->
//                    println(topic)
//                    println(qos)
//                    println(message)
//                }
//                println("subscribed, publish")
//                client.publish("helloyolo", QualityOfService.AT_LEAST_ONCE, "Meow")
//                println("published")
            }

    }
}


@Parcelize
object Logger :
    LogConfiguration(true, true, true, true, true) {
    override fun getLogClass(): Log {
        return AndroidLogger()
    }
}
