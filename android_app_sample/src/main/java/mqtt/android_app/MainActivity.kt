package mqtt.android_app

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProviders
import mqtt.client.connection.ConnectionParameters
import mqtt.client.viewmodel.SingleConnectionMqttClient
import mqtt.wire4.control.packet.ConnectionRequest

class MainActivity : AppCompatActivity() {
    val client by lazy { ViewModelProviders.of(this).get(SingleConnectionMqttClient::class.java) }
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        client.connectAsync(
            ConnectionParameters(
                "192.168.1.98", 60000, false,
                logConnectionAttempt = true,
                logOutgoingPublishOrSubscribe = true,
                logOutgoingControlPackets = true,
                logIncomingControlPackets = true,
                logIncomingPublish = true,
                connectionRequest = ConnectionRequest("yoloswag", keepAliveSeconds = 4.toUShort())
            )
        )
        setContentView(R.layout.activity_main)
    }
}
