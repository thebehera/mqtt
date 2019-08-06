package mqtt.android_app

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.ViewModelProviders
import mqtt.client.viewmodel.SingleConnectionMqttClient

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val fragmentActivity = this as FragmentActivity
        val client = ViewModelProviders.of(this).get(SingleConnectionMqttClient::class.java)

        setContentView(R.layout.activity_main)
    }
}
