package mqtt.client.service.ipc

import android.content.Context
import android.os.Bundle
import android.os.Message
import android.os.Messenger
import android.util.Log
import android.util.SparseArray
import androidx.databinding.Observable
import androidx.databinding.ObservableField
import mqtt.client.service.MESSAGE_PAYLOAD
import mqtt.client.service.ipc.ServiceToBoundClient.*
import mqtt.connection.*
import mqtt.wire.control.packet.ControlPacket
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

class ClientServiceNewConnectionManager(
    val context: Context, val bindManager: ClientServiceBindManager,
    val incomingMessenger: Messenger
) {
    private val continuationMap = SparseArray<((ConnectionState) -> Unit)>()
    private val mqttConnections = SparseArray<NonNullObservableField<ConnectionState>>()
    var incomingMessageCallback: ((ControlPacket, Int) -> Unit)? = null
    var outgoingMessageCallback: ((ControlPacket, Int) -> Unit)? = null

    suspend fun createConnection(
        remoteHost: IRemoteHost,
        awaitOnConnectionState: Int?
    ): NonNullObservableField<ConnectionState> {
        val connectionIdentifier = remoteHost.connectionIdentifier()
        val currentConnection = mqttConnections[connectionIdentifier]
        return if (currentConnection != null) {
            currentConnection
        } else {
            val observable = putOrUpdate(connectionIdentifier, Initializing)
            val messenger = bindManager.awaitServiceBound()
            val bundle = Bundle()
            bundle.putParcelable(MESSAGE_PAYLOAD, remoteHost)
            val message = Message()
            message.what = BoundClientToService.CREATE_CONNECTION.position
            message.data = bundle
            message.arg1 = remoteHost.connectionIdentifier()
            message.replyTo = incomingMessenger
            messenger.send(message)
            awaitConnectionStateChanged(remoteHost, awaitOnConnectionState)
            observable
        }
    }

    fun onMessage(msg: Message): Boolean {
        when (msg.what) {
            INCOMING_CONTROL_PACKET.position -> {
                val bundle = msg.data
                bundle.classLoader = javaClass.classLoader
                val incomingControlPacket = bundle.getParcelable<ControlPacket>(MESSAGE_PAYLOAD) ?: return false
                incomingMessageCallback?.invoke(incomingControlPacket, msg.arg1)
                return true
            }
            OUTGOING_CONTROL_PACKET.position -> {
                val bundle = msg.data
                bundle.classLoader = javaClass.classLoader
                try {
                    val outgoingControlPacket = bundle.getParcelable<ControlPacket>(MESSAGE_PAYLOAD) ?: return false
                    outgoingMessageCallback?.invoke(outgoingControlPacket, msg.arg1)
                } catch (e: IllegalAccessError) {
                    Log.e("MQTT", "Failed to forward outgoing packet", e)
                }
                return true
            }
            CONNECTION_STATE_CHANGED.position -> {
                val bundle = msg.data
                bundle.classLoader = javaClass.classLoader
                val updated =
                    bundle.getParcelable<IMqttConnectionStateUpdated>(MESSAGE_PAYLOAD) ?: return false
                val currentConnectionState = updated.state
                Log.i("MQTT", "New connection state recv by client process $currentConnectionState")
                putOrUpdate(updated.remoteHostConnectionIdentifier, updated.state)
                val connackHandler = continuationMap.get(updated.remoteHostConnectionIdentifier) ?: return true
                if (currentConnectionState is Open) {
                    continuationMap.remove(updated.remoteHostConnectionIdentifier)
                }
                connackHandler(currentConnectionState)
                return true
            }
        }
        return false
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
        remoteHost: IRemoteHost,
        awaitOnConnectionState: Int?
    ): ConnectionState =
        suspendCoroutine { continuation ->
            Log.i("RAHUL", "Await connection state change")
            val msgHandler = SuspendOnIncomingMessageHandler<ConnectionState>()
            msgHandler.queue(continuation)
            continuationMap.put(remoteHost.connectionIdentifier()) {

                Log.i("RAHUL", "Await connection state change continuation map put $it")
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