package mqtt.client.service.ipc

import android.os.Bundle
import android.os.Message
import android.os.Messenger
import mqtt.connection.IMqttConfiguration
import mqtt.connection.IMqttConnectionAcknowleged
import mqtt.wire.control.packet.IConnectionAcknowledgment
import kotlin.coroutines.suspendCoroutine

class ClientServiceNewConnectionManager(val bindManager: ClientServiceBindManager, val incomingMessenger: Messenger) {
    private val connectionAcknowledgmentContinuation =
        HashMap<Int, SuspendOnIncomingMessageHandler<IMqttConnectionAcknowleged>>()

    suspend fun createConnection(config: IMqttConfiguration): IConnectionAcknowledgment {
        val messenger = bindManager.awaitServiceBound()
        val bundle = Bundle()
        bundle.putParcelable("config", config)
        bundle.putString("Rahul", "Is a baller")

        val message = Message()
        message.what = BoundClientToService.CREATE_CONNECTION.ordinal
        message.data = bundle
        message.obj = config
        message.replyTo = incomingMessenger
        messenger.send(message)
        return awaitConnectionAcknowlegement(config).acknowledgment
    }

    suspend fun awaitConnectionAcknowlegement(config: IMqttConfiguration): IMqttConnectionAcknowleged =
        suspendCoroutine { continuation ->
            val msgHandler = SuspendOnIncomingMessageHandler<IMqttConnectionAcknowleged>()
            msgHandler.queue(continuation)
            connectionAcknowledgmentContinuation[config.remoteHost.connectionIdentifier()] = msgHandler
        }

    fun onMessage(msg: Message): Boolean {
        val mqttConnected = msg.obj as? IMqttConnectionAcknowleged ?: return false
        val continuation =
            connectionAcknowledgmentContinuation.remove(mqttConnected.remoteHostConnectionIdentifier) ?: return false
        continuation.notify(mqttConnected)
        return true
    }
}