package mqtt.client.service.ipc

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import mqtt.client.service.ConnectionManagerService
import mqtt.client.service.MqttConnectionsDatabaseDescriptor
import mqtt.connection.IRemoteHost
import mqtt.connection.Open
import mqtt.wire.control.packet.ControlPacket

abstract class AbstractMqttServiceViewModel(app: Application, dbDescriptor: MqttConnectionsDatabaseDescriptor) :
    AndroidViewModel(app), CoroutineScope {
    val job = Job()
    override val coroutineContext = Dispatchers.Main + job
    private val serviceConnection by lazy {
        ClientToServiceConnection(app, ConnectionManagerService::class.java, dbDescriptor)
    }

    /**
     * Create new managed mqtt connection
     */
    suspend fun createConnection(config: IRemoteHost, awaitOnConnectionState: Int? = Open.state) =
        serviceConnection.createNewConnection(config, awaitOnConnectionState)

    fun incomingMessageCallback(cb: (ControlPacket, Int) -> Unit) {
        serviceConnection.newConnectionManager.incomingMessageCallback = cb
    }

    fun messageSentCallback(cb: (ControlPacket, Int) -> Unit) {
        serviceConnection.newConnectionManager.outgoingMessageCallback = cb
    }

    suspend fun notifyPublish(notifyPublish: ClientToServiceConnection.NotifyPublish) {
        serviceConnection.notifyPublish(notifyPublish)
    }

    override fun onCleared() {
        serviceConnection.unbind(getApplication())
        super.onCleared()
    }
}

