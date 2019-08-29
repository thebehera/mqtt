package mqtt.client.service

import android.app.Service
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import mqtt.connection.IMqttConfiguration
import mqtt.persistence.IQueuedMessage
import mqtt.persistence.MqttPersistence
import kotlin.coroutines.CoroutineContext

interface OnConnectionListener {
    fun connect(connectionParameters: IMqttConfiguration)

}

interface OnQueueInvalidatedListener {

    fun onQueueInvalidated(persistence: MqttPersistence<out IQueuedMessage>)
}

abstract class CoroutineService : Service(), CoroutineScope {
    protected val job: Job = Job()
    protected val dispatcher = Dispatchers.Main
    override val coroutineContext: CoroutineContext = job + dispatcher
}
