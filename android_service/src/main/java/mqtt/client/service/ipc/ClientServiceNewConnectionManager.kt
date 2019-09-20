package mqtt.client.service.ipc

import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.os.Message
import android.os.Messenger
import android.util.Log
import android.util.SparseArray
import androidx.databinding.Observable
import androidx.databinding.ObservableField
import mqtt.Parcelable
import mqtt.client.service.MESSAGE_PAYLOAD
import mqtt.client.service.SingleConnection
import mqtt.connection.ConnectionState
import mqtt.connection.IMqttConfiguration
import mqtt.connection.IMqttConnectionStateUpdated
import mqtt.connection.Initializing
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

class ClientServiceNewConnectionManager(
    val context: Context,
    val bindManager: ClientServiceBindManager, val incomingMessenger: Messenger
) {
    private val continuationMap = SparseArray<((ConnectionState) -> Unit)>()
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
            val messenger = bindManager.serviceMessenger
            if (messenger != null) {
                Log.i("RAHUL", "Bound, Build msg")
                val message = Message()
                message.what = BoundClientToService.CREATE_CONNECTION.ordinal
                message.data = payloadToBundle(config)
                message.replyTo = incomingMessenger
                Log.i("RAHUL", "Sending $config")
                messenger.send(message)
            } else {
                val intent = Intent(context, SingleConnection::class.java)
                intent.putExtra(MESSAGE_PAYLOAD, config)
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    context.startForegroundService(intent)
                } else {
                    context.startService(intent)
                }
            }
            Log.i("RAHUL", "Awaiting connection status change")
            awaitConnectionStateChanged(config, awaitOnConnectionState)
            Log.i("RAHUL", "Connection status changed, return")
            observable
        }
    }

    fun payloadToBundle(payload: Parcelable): Bundle {
        val bundle = Bundle()
        bundle.putParcelable(MESSAGE_PAYLOAD, payload)
        return bundle
    }

    fun onMessage(msg: Message): Boolean {
        val bundle = msg.data
        bundle.classLoader = javaClass.classLoader
        val obj = bundle.getParcelable<Parcelable>(MESSAGE_PAYLOAD)
        val updated = obj as? IMqttConnectionStateUpdated ?: return false
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