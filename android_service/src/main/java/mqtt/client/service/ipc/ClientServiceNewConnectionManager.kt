package mqtt.client.service.ipc

import android.os.Message
import android.os.Messenger
import android.util.Log
import android.util.SparseArray
import androidx.databinding.Observable
import androidx.databinding.ObservableField
import mqtt.connection.ConnectionState
import mqtt.connection.IMqttConfiguration
import mqtt.connection.IMqttConnectionStateUpdated
import mqtt.connection.Initializing
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

class ClientServiceNewConnectionManager(val bindManager: ClientServiceBindManager, val incomingMessenger: Messenger) {
    private val continuationMap =
        SparseArray<((ConnectionState) -> Unit)>()
    private val mqttConnections = SparseArray<NonNullObservableField<ConnectionState>>()

    suspend fun createConnection(
        config: IMqttConfiguration,
        awaitOnConnectionState: Int?
    ): NonNullObservableField<ConnectionState> {
        val connectionIdentifier = config.remoteHost.connectionIdentifier()
        val currentConnection = mqttConnections[connectionIdentifier]
        return if (currentConnection != null) {
            currentConnection
        } else {
            val observable = putOrUpdate(connectionIdentifier, Initializing)
            Log.i("RAHUL", "Await Bind")
            val messenger = bindManager.awaitServiceBound()
            Log.i("RAHUL", "Bound, Build msg")
            val message = Message()
            message.what = BoundClientToService.CREATE_CONNECTION.ordinal
            message.obj = config
            message.replyTo = incomingMessenger
            Log.i("RAHUL", "Sending $config")
            messenger.send(message)
            Log.i("RAHUL", "Awaiting connection status change")
            awaitConnectionStateChanged(config, awaitOnConnectionState)
            Log.i("RAHUL", "Connection status changed, return")
            observable
        }
    }

    fun onMessage(msg: Message): Boolean {
        val updated = msg.obj as? IMqttConnectionStateUpdated ?: return false
        val currentConnectionState = updated.state
        Log.i("RAHUL", "On msg update: $currentConnectionState")
        putOrUpdate(updated.remoteHostConnectionIdentifier, updated.state)
        Log.i("RAHUL", "Connection Updated")
        val connackHandler = continuationMap.get(updated.remoteHostConnectionIdentifier) ?: return true
        continuationMap.remove(updated.remoteHostConnectionIdentifier)
        Log.i("RAHUL", "Notify")
        connackHandler(currentConnectionState)
        return true
    }

    private fun putOrUpdate(
        connectionIdentifier: Int,
        state: ConnectionState = Initializing
    ): NonNullObservableField<ConnectionState> {
        val observableFound = mqttConnections.get(connectionIdentifier)
        return if (observableFound == null) {
            val observable = NonNullObservableField(state)
            mqttConnections.put(connectionIdentifier, observable)
            observable
        } else {
            observableFound.set(state)
            observableFound
        }
    }


    private suspend fun awaitConnectionStateChanged(
        config: IMqttConfiguration,
        awaitOnConnectionState: Int?
    ): ConnectionState =
        suspendCoroutine { continuation ->
            val msgHandler = SuspendOnIncomingMessageHandler<ConnectionState>()
            msgHandler.queue(continuation)
            continuationMap.put(config.remoteHost.connectionIdentifier()) {
                if (awaitOnConnectionState == null || awaitOnConnectionState == it.state) {
                    continuation.resume(it)
                }
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
