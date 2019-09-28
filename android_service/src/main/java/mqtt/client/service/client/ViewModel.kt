package mqtt.client.service.client

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import mqtt.client.service.SingleConnection
import mqtt.client.service.ipc.ClientToServiceConnection
import mqtt.connection.IMqttConfiguration
import mqtt.connection.Open
import mqtt.wire.control.packet.ControlPacket

class MqttServiceViewModel(app: Application) : AndroidViewModel(app), CoroutineScope {
    val job = Job()
    override val coroutineContext = Dispatchers.Main + job
    private val serviceConnection by lazy { ClientToServiceConnection(app, SingleConnection::class.java) }

    /**
     * Create new managed mqtt connection
     */
    suspend fun createConnection(config: IMqttConfiguration, awaitOnConnectionState: Int? = Open.state) =
        serviceConnection.createNewConnection(config, awaitOnConnectionState)

    fun setCallback(cb: (ControlPacket, Int) -> Unit) = serviceConnection.setCallback(cb)

    override fun onCleared() {
        serviceConnection.unbind(getApplication())
        super.onCleared()
    }


}
