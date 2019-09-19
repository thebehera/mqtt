package mqtt.client.service.ipc

import android.os.Message
import android.os.Messenger
import android.util.SparseArray
import androidx.databinding.Observable
import androidx.databinding.ObservableField
import mqtt.connection.ConnectionState
import mqtt.connection.IMqttConfiguration
import mqtt.connection.IMqttConnectionStateUpdated
import mqtt.connection.Initializing
import kotlin.coroutines.suspendCoroutine

class ClientServiceNewConnectionManager(val bindManager: ClientServiceBindManager, val incomingMessenger: Messenger) {
    private val continuationMap =
        SparseArray<SuspendOnIncomingMessageHandler<ConnectionState>>()
    val mqttConnections = SparseArray<NonNullObservableField<ConnectionState>>()


    suspend fun createConnection(config: IMqttConfiguration): NonNullObservableField<ConnectionState> {
        val connectionIdentifier = config.remoteHost.connectionIdentifier()
        val currentConnection = mqttConnections[connectionIdentifier]
        return if (currentConnection != null) {
            currentConnection
        } else {
            val observable = NonNullObservableField<ConnectionState>(Initializing)

            mqttConnections.put(connectionIdentifier, observable)
            val messenger = bindManager.awaitServiceBound()
            val message = Message()
            message.what = BoundClientToService.CREATE_CONNECTION.ordinal
            message.obj = config
            message.replyTo = incomingMessenger
            messenger.send(message)
            awaitConnectionStateChanged(config)
            observable
        }
    }

    private suspend fun awaitConnectionStateChanged(config: IMqttConfiguration): ConnectionState =
        suspendCoroutine { continuation ->
            val msgHandler = SuspendOnIncomingMessageHandler<ConnectionState>()
            msgHandler.queue(continuation)
            continuationMap.put(config.remoteHost.connectionIdentifier(), msgHandler)
        }

    fun onMessage(msg: Message): Boolean {
        val updated = msg.obj as? IMqttConnectionStateUpdated ?: return false
        val currentConnectionState = updated.state
        updateMqttConnection(updated)
        val connackHandler = continuationMap.get(updated.remoteHostConnectionIdentifier) ?: return true
        connackHandler.notify(currentConnectionState)
        return true
    }

    private fun updateMqttConnection(updated: IMqttConnectionStateUpdated) {
        val currentConnectionObservable = mqttConnections[updated.remoteHostConnectionIdentifier]
        if (currentConnectionObservable == null) {
            mqttConnections.put(updated.remoteHostConnectionIdentifier, NonNullObservableField(updated.state))
        } else {
            currentConnectionObservable.set(updated.state)
        }
    }
}

class NonNullObservableField<T : Any>(value: T, vararg dependencies: Observable) : ObservableField<T>(*dependencies) {
    init {
        set(value)
    }

    override fun get(): T = super.get()!!
    @Suppress("RedundantOverride") // Only allow non-null `value`.
    override fun set(value: T) = super.set(value)
}
