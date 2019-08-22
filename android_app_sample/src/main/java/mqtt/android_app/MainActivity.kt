package mqtt.android_app

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProviders
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import mqtt.Log
import mqtt.Parcelize
import mqtt.android_app.room.AndroidConfiguration
import mqtt.android_app.room.initQueuedDb
import mqtt.client.connection.parameters.LogConfiguration
import mqtt.client.connection.parameters.RemoteHost
import mqtt.client.viewmodel.SimpleMqttClientViewModel
import mqtt.wire.data.QualityOfService
import mqtt.wire4.control.packet.ConnectionRequest

class MainActivity : AppCompatActivity() {
    val client by lazy { ViewModelProviders.of(this).get(SimpleMqttClientViewModel::class.java) }
    override fun onCreate(savedInstanceState: Bundle?) {
        initQueuedDb(this)
        super.onCreate(savedInstanceState)
        val connection = client.connectAsync(
            AndroidConfiguration(
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
        )
        GlobalScope
            .launch {
                println("await")
                connection.await()
                println("subscribe")
                client.subscribe<String>("helloyolo", QualityOfService.AT_LEAST_ONCE) { topic, qos, message ->
                    println(topic)
                    println(qos)
                    println(message)
                }
                println("subscribed, publish")
                client.publish("helloyolo", QualityOfService.AT_LEAST_ONCE, "Meow")
                println("published")
            }
        setContentView(R.layout.activity_main)
    }
}


@Parcelize
object Logger :
    LogConfiguration(true, true, true, true, true) {
    override fun getLogClass(): Log {
        return AndroidLogger()
    }
}
