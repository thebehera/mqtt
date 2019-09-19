package mqtt.client.service.ipc

import android.os.Message
import android.os.Messenger
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import mqtt.connection.ConnectionState
import mqtt.connection.IMqttConfiguration
import mqtt.connection.IMqttConnectionStateUpdated
import mqtt.connection.Initializing
import kotlin.coroutines.suspendCoroutine

class ClientServiceNewConnectionManager(val bindManager: ClientServiceBindManager, val incomingMessenger: Messenger) {
    private val continuationMap =
        HashMap<Int, SuspendOnIncomingMessageHandler<ConnectionState>>()
    val mqttConnections = HashMap<Int, NonNullObservableField<ConnectionState>>()

    val connections = HashMap<Int, Flow<ConnectionState>>()


    suspend fun createConnection(config: IMqttConfiguration): Flow<ConnectionState> {
        val connectionIdentifier = config.remoteHost.connectionIdentifier()
        val currentConnection = connections[connectionIdentifier]

        return if (currentConnection != null) {
            currentConnection
        } else {
            val messenger = bindManager.awaitServiceBound()
            val message = Message()
            message.what = BoundClientToService.CREATE_CONNECTION.ordinal
            message.obj = config
            message.replyTo = incomingMessenger
            messenger.send(message)
            awaitConnectionStateChanged(config)
            val flow = flow<ConnectionState> {
                emit(Initializing)
            }
            connections[connectionIdentifier] = flow
            flow
        }
    }

    private suspend fun awaitConnectionStateChanged(config: IMqttConfiguration): ConnectionState =
        suspendCoroutine { continuation ->
            val msgHandler = SuspendOnIncomingMessageHandler<ConnectionState>()
            msgHandler.queue(continuation)
            continuationMap[config.remoteHost.connectionIdentifier()] = msgHandler
        }

    fun onMessage(msg: Message): Boolean {
        val updated = msg.obj as? IMqttConnectionStateUpdated ?: return false
        val currentConnectionState = updated.state
        updateMqttConnection(updated)
        val connackHandler = continuationMap.remove(updated.remoteHostConnectionIdentifier) ?: return true
        connackHandler.notify(currentConnectionState)
        return true
    }

    private fun updateMqttConnection(updated: IMqttConnectionStateUpdated) {
        val currentFlow = connections[updated.remoteHostConnectionIdentifier]

        if (currentFlow == null) {
            connections[updated.remoteHostConnectionIdentifier] = flow<ConnectionState> {
                emit(Initializing)
            }
        } else {
            currentFlow
        }
        val currentConnectionObservable = mqttConnections[updated.remoteHostConnectionIdentifier]
        if (currentConnectionObservable == null) {
            mqttConnections[updated.remoteHostConnectionIdentifier] = NonNullObservableField(updated.state)
        } else {
            currentConnectionObservable.set(updated.state)
        }
    }
}