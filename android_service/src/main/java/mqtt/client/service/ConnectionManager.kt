package mqtt.client.service

import android.app.Service
import android.content.Intent
import android.os.IBinder
import android.os.Messenger
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import mqtt.Log
import mqtt.client.connection.ConnectionParameters
import mqtt.wire.data.QualityOfService
import mqtt.wire.data.topic.Name

class ConnectionManager : Service(), CoroutineScope, OnClientMessageReceived {
    private val job: Job = Job()
    override val coroutineContext = job + Dispatchers.Main

    private val messenger by lazy { Messenger(IncomingHandler(this)) }
    private val connections = LinkedHashMap<ConnectionParameters, SingleConnection>()
    private lateinit var log: Log

    override fun onBind(intent: Intent): IBinder = messenger.binder


    override fun onMessage(obj: Any) {
        when (obj) {
            is Number -> {

            }
        }
    }

    suspend fun connect(params: ConnectionParameters) {
        if (connections.containsKey(params)) {
            return
        }
        val connection = SingleConnection(params, log)
        connections[params] = connection
        connection.connect().await()
    }

    private suspend inline fun <reified T : Any> publish(
        params: ConnectionParameters,
        topic: String,
        qos: QualityOfService,
        obj: T
    ) {
        val connection = connections[params]
        if (connection == null) {
            log.warning("ConnectionManager", "Failed to find connection for parameters $params")
            return
        }
        connection.publish(topic, qos, obj)
    }

    private suspend inline fun <reified T : Any> subscribe(
        params: ConnectionParameters, topicFilter: String, qos: QualityOfService,
        crossinline callback: (topic: Name, qos: QualityOfService, message: T?) -> Unit
    ) {
        val connection = connections[params]
        if (connection == null) {
            log.warning("ConnectionManager", "Failed to find connection for parameters $params")
            return
        }
        connection.subscribe(topicFilter, qos, callback)
    }

    private suspend fun disconnect(params: ConnectionParameters) {
        val connection = connections[params]
        if (connection == null) {
            log.warning("ConnectionManager", "Failed to find connection for parameters $params")
            return
        }
        connection.disconnect().await()
    }


    override fun onDestroy() {
        super.onDestroy()
        job.cancel("Service is destroyed")
    }

}
