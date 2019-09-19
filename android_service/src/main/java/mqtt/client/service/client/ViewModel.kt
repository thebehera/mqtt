package mqtt.client.service.client

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import mqtt.client.service.SingleConnection
import mqtt.client.service.ipc.ClientToServiceConnection
import mqtt.connection.IMqttConfiguration

class MqttServiceViewModel(app: Application) : AndroidViewModel(app), CoroutineScope {
    val job = Job()
    override val coroutineContext = Dispatchers.Main + job
    private val serviceConnection by lazy { ClientToServiceConnection(SingleConnection::class.java) }

    init {
        serviceConnection.bind(getApplication())
    }

    /**
     * Create new managed mqtt connection
     */
    suspend fun createConnection(config: IMqttConfiguration) = serviceConnection.createNewConnection(config)

    override fun onCleared() {
        serviceConnection.unbind(getApplication())
        super.onCleared()
    }


}
